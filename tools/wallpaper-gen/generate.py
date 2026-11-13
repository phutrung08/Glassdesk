"""Tạo bộ hình nền cho Glassdeck, chỉ dùng numpy (không cần thư viện ảnh).

Mọi hình đều được vẽ bằng thuật toán trong file này, nên thuộc quyền của dự án
(MIT, xem LICENSE). Chạy:

    python -I generate.py <thư_mục_ra>

Kết quả là các file PNG 1920x1080; tools/wallpaper-gen/convert.ps1 đổi sang JPEG.
"""
import struct
import sys
import zlib
from pathlib import Path

import numpy as np

W, H = 1920, 1080
ASPECT = W / H
RNG_SEED = 2026

# Toạ độ chuẩn hoá: x trong [0, ASPECT], y trong [0, 1] (y tăng xuống dưới).
YY, XX = np.mgrid[0:H, 0:W].astype(np.float32)
X = XX / H
Y = YY / H
PX = 1.0 / H  # một điểm ảnh trong toạ độ chuẩn hoá


# ---------- tiện ích ----------

def hexc(s):
    s = s.lstrip('#')
    return np.array([int(s[i:i + 2], 16) / 255.0 for i in (0, 2, 4)], dtype=np.float32)


def smoothstep(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def mix(a, b, t):
    t = np.asarray(t, dtype=np.float32)
    if t.ndim == 2:
        t = t[..., None]
    return a * (1 - t) + b * t


def ramp(t, stops):
    """Dải màu nhiều điểm: stops = [(vị trí, màu), ...] theo thứ tự tăng dần."""
    t = np.clip(t, 0.0, 1.0)
    out = np.zeros(t.shape + (3,), dtype=np.float32)
    out[:] = stops[0][1]
    for (p0, c0), (p1, c1) in zip(stops, stops[1:]):
        k = smoothstep(p0, p1, t)
        out = np.where(((t >= p0)[..., None]), mix(c0, c1, k), out)
    return out


def box_blur(img, r):
    """Làm mờ hộp tách trục, 3 lượt ~ Gaussian. r tính bằng điểm ảnh."""
    r = int(r)
    if r <= 0:
        return img
    out = img
    for _ in range(3):
        for axis in (0, 1):
            pad = [(0, 0)] * out.ndim
            pad[axis] = (r + 1, r)
            p = np.pad(out, pad, mode='edge')
            c = np.cumsum(p, axis=axis, dtype=np.float64)
            n = out.shape[axis]
            hi = np.take(c, np.arange(2 * r + 1, 2 * r + 1 + n), axis=axis)
            lo = np.take(c, np.arange(0, n), axis=axis)
            out = ((hi - lo) / (2 * r + 1)).astype(np.float32)
    return out


def finish(img, grain=0.012, seed=0):
    """Thêm nhiễu hạt rất nhẹ để chống vệt dải màu, rồi chuyển sang 8 bit."""
    rng = np.random.default_rng(RNG_SEED + seed)
    img = img + rng.normal(0, grain, img.shape[:2])[..., None].astype(np.float32)
    return (np.clip(img, 0, 1) * 255 + 0.5).astype(np.uint8)


def save_png(path, rgb8):
    h, w, _ = rgb8.shape
    raw = b''.join(b'\x00' + rgb8[y].tobytes() for y in range(h))

    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b'\x89PNG\r\n\x1a\n'
    png += chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(raw, 6))
    png += chunk(b'IEND', b'')
    Path(path).write_bytes(png)


# ---------- 1. Lụa mềm: các dải vải cong chồng lên nhau ----------

def silk(bg_stops, bg_angle, layers, angle, final_blur=2, seed=0):
    """bg_stops: dải màu nền; layers: các dải lụa từ sau ra trước."""
    ca, sa = np.cos(bg_angle), np.sin(bg_angle)
    t = (X * ca + Y * sa)
    t = (t - t.min()) / (t.max() - t.min())
    img = ramp(t, bg_stops)

    c, s = np.cos(angle), np.sin(angle)
    u = X * c + Y * s           # dọc theo dải
    v = -X * s + Y * c          # vuông góc với dải
    umin, umax = u.min(), u.max()
    un = (u - umin) / (umax - umin)

    for L in layers:
        if L.get('blur_before', 0):
            img = box_blur(img, L['blur_before'])
        edge = L['base'] + sum(a * np.sin(2 * np.pi * f * un + p) for a, f, p in L['waves'])
        slope = sum(a * 2 * np.pi * f * np.cos(2 * np.pi * f * un + p) for a, f, p in L['waves'])
        d = v - edge                                   # >0: bên trong dải
        alpha = smoothstep(-1.2 * PX, 1.2 * PX, d)
        # Màu dải: đổi theo chiều dọc dải và đậm dần vào trong.
        col = ramp(un, L['stops'])
        depth = smoothstep(0, L.get('fold', 0.18), d)
        col = col * (1 - L.get('inner_dark', 0.25) * depth)[..., None]
        # Ánh sáng theo độ dốc của mép: chỗ mép đi lên sáng hơn, đi xuống tối hơn,
        # tắt dần vào trong, tạo cảm giác tấm lụa có nếp gấp.
        light = np.tanh(-slope * L.get('slope_gain', 2.5)) * L.get('slope_light', 0.28)
        fall = np.exp(-np.maximum(d, 0) / L.get('light_depth', 0.22))
        col = col * (1 + light * fall)[..., None]
        # Dải sáng bóng chạy song song với mép, hơi lùi vào trong.
        band = np.exp(-((d - L.get('band_at', 0.05)) / L.get('band_w', 0.02)) ** 2) * L.get('band', 0.0)
        col = col + (1 - col) * (band * np.clip(0.5 + light, 0, 1))[..., None]
        # Viền sáng bóng ngay mép, và vùng sáng mềm ngay sau viền.
        rim = np.exp(-np.maximum(d, 0) / (L.get('rim_w', 2.5) * PX)) * L.get('rim', 0.55)
        sheen = np.exp(-np.maximum(d, 0) / 0.03) * L.get('sheen', 0.18)
        col = col + (L.get('rim_color', hexc('#ffffff')) - col) * np.clip(rim + sheen, 0, 1)[..., None]
        # Bóng đổ lên lớp phía sau.
        shadow = np.exp(-np.maximum(-d, 0) / L.get('shadow_w', 0.035)) * L.get('shadow', 0.35)
        img = img * (1 - shadow * (d < 0))[..., None]
        img = mix(img, col, alpha)

    img = box_blur(img, final_blur)
    return finish(img, seed=seed)


def silk_set():
    W_ = hexc('#ffffff')
    out = {}
    # 1. Hoàng hôn: trời xanh nhạt, dải cam vàng uốn lượn, dải hồng tím phía trước.
    out['lua-hoang-hon'] = silk(
        [(0, hexc('#b9ecf7')), (0.6, hexc('#86d3ec')), (1, hexc('#d8f3fa'))], 0.3,
        [
            dict(base=0.22, waves=[(0.16, 0.7, 0.9), (0.05, 1.9, 2.2), (0.02, 3.7, 0.4)],
                 stops=[(0, hexc('#ffc23a')), (0.45, hexc('#ff9a12')), (1, hexc('#ff6a2b'))],
                 rim=0.55, sheen=0.2, inner_dark=0.18, band=0.35, band_at=0.06, slope_light=0.32),
            dict(base=0.52, waves=[(0.14, 0.6, 2.6), (0.05, 1.6, 0.6), (0.015, 4.1, 1.7)],
                 stops=[(0, hexc('#ff5e8a')), (0.45, hexc('#e0479f')), (1, hexc('#6b23a8'))],
                 rim=0.5, sheen=0.15, inner_dark=0.3, band=0.25, band_at=0.05, blur_before=3),
            dict(base=0.86, waves=[(0.10, 0.5, 1.4), (0.03, 1.8, 2.9)],
                 stops=[(0, hexc('#8b3fd1')), (1, hexc('#3b1a8f'))],
                 rim=0.4, sheen=0.1, inner_dark=0.25, band=0.2, blur_before=2),
        ], angle=-0.30, seed=1)

    # 2. Đại dương: xanh thẫm, các lớp ngọc lam và bọt trắng.
    out['lua-dai-duong'] = silk(
        [(0, hexc('#020a22')), (0.5, hexc('#062457')), (1, hexc('#0b3b78'))], 1.1,
        [
            dict(base=0.30, waves=[(0.13, 0.6, 1.0), (0.04, 1.9, 2.0), (0.015, 4.3, 0.2)],
                 stops=[(0, hexc('#0b5fb0')), (1, hexc('#1290c8'))],
                 rim=0.35, sheen=0.12, inner_dark=0.4, band=0.2, slope_light=0.35),
            dict(base=0.50, waves=[(0.14, 0.55, 3.1), (0.035, 2.4, 0.8)],
                 stops=[(0, hexc('#25e0d6')), (0.6, hexc('#0fb0cc')), (1, hexc('#0b58ad'))],
                 rim=0.6, sheen=0.2, inner_dark=0.38, band=0.4, band_at=0.045, blur_before=4),
            dict(base=0.74, waves=[(0.12, 0.65, 0.2), (0.03, 1.6, 2.2)],
                 stops=[(0, hexc('#f2fcff')), (1, hexc('#a6e3f3'))],
                 rim=0.3, sheen=0.1, inner_dark=0.3, fold=0.25, blur_before=3, shadow=0.5,
                 band=0.3, slope_light=0.18),
            dict(base=0.90, waves=[(0.08, 0.6, 1.9), (0.02, 2.2, 0.4)],
                 stops=[(0, hexc('#0a7fb8')), (1, hexc('#063b7a'))],
                 rim=0.5, sheen=0.15, inner_dark=0.3, band=0.25, blur_before=0),
        ], angle=0.26, seed=2)

    # 3. Cực quang: nền đêm tím than, dải xanh ngọc và tím phát sáng.
    out['lua-cuc-quang'] = silk(
        [(0, hexc('#0a0420')), (0.55, hexc('#170a38')), (1, hexc('#03121b'))], 0.8,
        [
            dict(base=0.20, waves=[(0.14, 0.5, 0.7), (0.05, 1.8, 2.5), (0.015, 4.0, 1.0)],
                 stops=[(0, hexc('#2af0aa')), (0.5, hexc('#16bfa8')), (1, hexc('#0d6f8f'))],
                 rim=0.45, sheen=0.3, inner_dark=0.5, band=0.45, band_at=0.04, shadow=0.55,
                 slope_light=0.35),
            dict(base=0.48, waves=[(0.13, 0.6, 2.0), (0.04, 2.2, 0.1)],
                 stops=[(0, hexc('#5b21b6')), (0.5, hexc('#9b4dff')), (1, hexc('#e47cff'))],
                 rim=0.55, sheen=0.2, inner_dark=0.45, band=0.4, blur_before=5, shadow=0.6),
            dict(base=0.80, waves=[(0.10, 0.55, 3.0), (0.03, 1.9, 1.2)],
                 stops=[(0, hexc('#1b0b46')), (1, hexc('#3a1580'))],
                 rim=0.35, sheen=0.1, inner_dark=0.2, band=0.3, blur_before=2, shadow=0.6),
        ], angle=-0.42, seed=3)

    # 4. Hồng đào: pastel nhẹ nhàng, hợp phòng sáng.
    out['lua-hong-dao'] = silk(
        [(0, hexc('#fff0e6')), (0.5, hexc('#fdd9cc')), (1, hexc('#f8c0cf'))], 0.6,
        [
            dict(base=0.26, waves=[(0.13, 0.7, 1.7), (0.04, 2.0, 0.6), (0.012, 4.4, 2.2)],
                 stops=[(0, hexc('#ffc2a3')), (1, hexc('#ff9a8f'))],
                 rim=0.6, sheen=0.3, inner_dark=0.12, band=0.35, shadow=0.2, slope_light=0.22),
            dict(base=0.55, waves=[(0.13, 0.55, 0.4), (0.04, 1.7, 2.9)],
                 stops=[(0, hexc('#f784b2')), (0.6, hexc('#ea5f96')), (1, hexc('#c04a95'))],
                 rim=0.55, sheen=0.25, inner_dark=0.18, band=0.35, blur_before=3, shadow=0.24),
            dict(base=0.85, waves=[(0.09, 0.6, 2.2), (0.025, 2.1, 0.9)],
                 stops=[(0, hexc('#fff4ef')), (1, hexc('#f6d6e2'))],
                 rim=0.3, sheen=0.1, inner_dark=0.1, band=0.2, blur_before=2, shadow=0.22),
        ], angle=0.20, seed=4)

    # 5. Lửa và băng: nền xanh hoàng gia, dải đỏ cam rực và dải trắng sứ.
    out['lua-lua-bang'] = silk(
        [(0, hexc('#081a5e')), (0.5, hexc('#1448b5')), (1, hexc('#2a78dc'))], -0.6,
        [
            dict(base=0.10, waves=[(0.15, 0.6, 2.6), (0.05, 1.5, 0.9), (0.015, 3.9, 0.3)],
                 stops=[(0, hexc('#ff2d55')), (0.45, hexc('#ff6a1a')), (1, hexc('#ffc23a'))],
                 rim=0.5, sheen=0.25, inner_dark=0.38, band=0.4, shadow=0.55, slope_light=0.35),
            dict(base=0.30, waves=[(0.14, 0.55, 0.9), (0.04, 2.3, 2.0)],
                 stops=[(0, W_), (1, hexc('#ddd6f2'))],
                 rim=0.2, sheen=0.1, inner_dark=0.32, fold=0.06, blur_before=4, shadow=0.6,
                 band=0.0, slope_light=0.25),
            dict(base=0.36, waves=[(0.14, 0.55, 0.9), (0.04, 2.3, 2.0)],
                 stops=[(0, hexc('#d63cff')), (0.5, hexc('#7b2ff7')), (1, hexc('#1d4ed8'))],
                 rim=0.45, sheen=0.2, inner_dark=0.35, band=0.35, shadow=0.45),
        ], angle=0.50, seed=5)
    return out


# ---------- 2. Kiểu Tidyo: dải màu chéo tối dần ----------

def tidyo(brand, seed, start=None, mid=None):
    b = hexc(brand)
    black = hexc('#07080c')
    s = hexc(start) if start else b * 0.55
    m = hexc(mid) if mid else b * 0.33 + black * 0.4
    t = (X / ASPECT * 0.62 + Y * 0.38)
    img = ramp(t, [(0, s), (0.5, m), (1, black)])
    # Quầng sáng rất nhẹ ở góc trên trái để nền có chiều sâu.
    glow = np.exp(-(((X - 0.15) ** 2) / 0.35 + ((Y - 0.05) ** 2) / 0.25)) * 0.22
    img = img + b[None, None, :] * glow[..., None] * 0.6
    vign = smoothstep(1.35, 0.5, np.sqrt(((X - ASPECT / 2) / ASPECT) ** 2 * 2.2 + (Y - 0.5) ** 2 * 1.6))
    img = img * (0.82 + 0.18 * vign)[..., None]
    return finish(img, grain=0.008, seed=seed)


def tidyo_set():
    colors = {
        'tidyo-xanh-duong': '#1877f2',
        'tidyo-xanh-la': '#1db954',
        'tidyo-do': '#e50914',
        'tidyo-tim': '#8e44ec',
        'tidyo-vang': ('#f7c600', '#a86f00', '#4a2c02'),
        'tidyo-cam': ('#ff7a00', '#c9500a', '#5c1f06'),
    }
    out = {}
    for i, (name, c) in enumerate(colors.items()):
        if isinstance(c, tuple):
            out[name] = tidyo(c[0], i + 10, start=c[1], mid=c[2])
        else:
            out[name] = tidyo(c, i + 10)
    return out


# ---------- 3. Hoạt hình phẳng ----------

def paint(img, mask, color):
    m = np.clip(mask, 0, 1)[..., None]
    return img * (1 - m) + color * m


def ridge(base, waves, peaks, xs):
    """Đường sống núi: sóng mềm cộng các đỉnh nhọn."""
    y = np.full_like(xs, base)
    for a, f, p in waves:
        y += a * np.sin(2 * np.pi * f * xs / ASPECT + p)
    for cx, h, w in peaks:
        y -= h * np.clip(1 - np.abs(xs - cx) / w, 0, 1) ** 1.6
    return y


def pine(img, x0, y0, h, w, color):
    """Cây thông ba tầng, vẽ trong khung nhỏ quanh cây cho nhanh."""
    x_lo, x_hi = int((x0 - w) * H), int((x0 + w) * H) + 2
    y_lo, y_hi = int((y0 - h) * H), int((y0 + h * 0.12) * H) + 2
    x_lo, y_lo = max(x_lo, 0), max(y_lo, 0)
    x_hi, y_hi = min(x_hi, W), min(y_hi, H)
    if x_lo >= x_hi or y_lo >= y_hi:
        return img
    xs, ys = X[y_lo:y_hi, x_lo:x_hi], Y[y_lo:y_hi, x_lo:x_hi]
    t = (ys - (y0 - h)) / h
    tier = (t * 3) % 1
    half = w * t * (0.55 + 0.45 * tier)
    m = smoothstep(half + PX, half - PX, np.abs(xs - x0)) * (t >= 0) * (t <= 1)
    trunk = (np.abs(xs - x0) < w * 0.08) * (t > 1) * (t < 1.12)
    sub = img[y_lo:y_hi, x_lo:x_hi]
    img[y_lo:y_hi, x_lo:x_hi] = paint(sub, np.maximum(m, trunk), color)
    return img


def cartoon_mountains():
    xs = X
    img = ramp(Y / 0.62, [(0, hexc('#1b1f4b')), (0.45, hexc('#5b3a7a')), (0.8, hexc('#e0787a')),
                          (1, hexc('#ffc58a'))])
    rng = np.random.default_rng(RNG_SEED + 21)
    # Sao nhỏ ở phần trời cao, mờ dần xuống dưới.
    stars = np.zeros((H, W), dtype=np.float32)
    for _ in range(260):
        sx, sy = rng.integers(0, W), rng.integers(0, int(H * 0.38))
        stars[sy, sx] = rng.uniform(0.4, 1.0)
    stars = box_blur(stars[..., None], 1)[..., 0] * 6
    img = img + (stars * smoothstep(0.42, 0.05, Y))[..., None]
    # Mặt trời thấp, có quầng.
    sx, sy, sr = ASPECT * 0.68, 0.50, 0.085
    dist = np.sqrt((X - sx) ** 2 + (Y - sy) ** 2)
    img = img + hexc('#ffb36b') * (np.exp(-dist / 0.18) * 0.45)[..., None]
    img = paint(img, smoothstep(sr + PX, sr - PX, dist), hexc('#ffe1a8'))

    layers = [
        (0.56, [(0.02, 1.3, 0.4), (0.01, 3.1, 1.1)], [(0.4, 0.12, 0.35), (1.25, 0.09, 0.3)], '#8a5a8f'),
        (0.63, [(0.025, 1.0, 2.1), (0.012, 2.7, 0.2)], [(0.9, 0.14, 0.4), (1.6, 0.1, 0.28)], '#6a4078'),
        (0.71, [(0.03, 0.8, 1.4), (0.012, 2.3, 2.6)], [(0.25, 0.1, 0.3), (1.35, 0.12, 0.35)], '#4a2c5e'),
        (0.80, [(0.025, 0.9, 0.7), (0.01, 2.9, 1.9)], [], '#2f1d42'),
        (0.89, [(0.02, 1.1, 2.9), (0.008, 3.3, 0.5)], [], '#1b1128'),
    ]
    ridges = []
    for base, waves, peaks, col in layers:
        top = ridge(base, waves, peaks, xs[0])
        ridges.append(top)
        m = smoothstep(top - PX, top + PX, Y)
        # Sương mỏng ở chân mỗi lớp núi.
        haze = smoothstep(top + 0.0, top + 0.12, Y) * 0.0
        img = paint(img, m, hexc(col) + haze[..., None])
    # Rừng thông trên hai lớp trước.
    for layer_idx, n, hscale, col in ((3, 34, 0.055, '#24173a'), (4, 26, 0.085, '#120b1d')):
        top = ridges[layer_idx]
        for _ in range(n):
            x0 = rng.uniform(0, ASPECT)
            y0 = top[min(int(x0 * H), W - 1)] + 0.01
            h = hscale * rng.uniform(0.7, 1.25)
            img = pine(img, x0, y0, h, h * 0.32, hexc(col))
    # Vài con chim nhỏ.
    for bx, by, bs in ((0.55, 0.30, 0.012), (0.6, 0.27, 0.009), (0.64, 0.31, 0.010)):
        for side in (-1, 1):
            ax = X - bx
            m = (np.abs(Y - (by - np.abs(ax) * 0.6)) < 0.0018) * (side * ax > 0) * (np.abs(ax) < bs)
            img = paint(img, m.astype(np.float32), hexc('#2a1b3d'))
    return finish(img, grain=0.006, seed=21)


def cartoon_city():
    rng = np.random.default_rng(RNG_SEED + 22)
    horizon = 0.66
    sky = ramp(Y / horizon, [(0, hexc('#070b1f')), (0.55, hexc('#1a1b4b')), (0.85, hexc('#3b2a6b')),
                             (1, hexc('#7a3f7d'))])
    img = sky.copy()
    stars = np.zeros((H, W), dtype=np.float32)
    for _ in range(320):
        sx, sy = rng.integers(0, W), rng.integers(0, int(H * horizon * 0.8))
        stars[sy, sx] = rng.uniform(0.3, 1.0)
    stars = box_blur(stars[..., None], 1)[..., 0] * 6
    img = img + (stars * smoothstep(horizon, 0.1, Y))[..., None]
    # Trăng khuyết.
    mx, my, mr = ASPECT * 0.78, 0.2, 0.06
    d1 = np.sqrt((X - mx) ** 2 + (Y - my) ** 2)
    d2 = np.sqrt((X - mx - 0.028) ** 2 + (Y - my + 0.012) ** 2)
    img = img + hexc('#f6e7b0') * (np.exp(-d1 / 0.12) * 0.18)[..., None]
    moon = smoothstep(mr + PX, mr - PX, d1) * smoothstep(mr - PX, mr + PX, d2)
    img = paint(img, moon, hexc('#fbefc4'))

    # Ba lớp nhà, xa nhạt và thấp, gần đậm và cao.
    city = np.zeros((H, W), dtype=np.float32)
    for depth, (n, hmin, hmax, col, win) in enumerate([
        (40, 0.08, 0.20, '#2c2456', 0.10),
        (30, 0.10, 0.30, '#1f1940', 0.18),
        (22, 0.12, 0.36, '#120e26', 0.28),
    ]):
        x = -0.02
        while x < ASPECT:
            bw = rng.uniform(0.035, 0.085) * (1 + depth * 0.2)
            bh = rng.uniform(hmin, hmax)
            x0, x1 = x, x + bw
            y0 = horizon - bh
            xi0, xi1 = int(max(x0, 0) * H), int(min(x1, ASPECT) * H)
            yi0, yi1 = int(y0 * H), int(horizon * H)
            if xi1 > xi0:
                img[yi0:yi1, xi0:xi1] = hexc(col)
                city[yi0:yi1, xi0:xi1] = 1
                # Cửa sổ sáng ngẫu nhiên.
                cw, ch = 0.006, 0.009
                for wy in np.arange(y0 + 0.015, horizon - 0.02, 0.022):
                    for wx in np.arange(x0 + 0.01, x1 - 0.01, 0.016):
                        if rng.random() < win:
                            a, b = int(wy * H), int((wy + ch) * H)
                            c, e = int(wx * H), int((wx + cw) * H)
                            img[a:b, max(c, 0):min(e, W)] = hexc('#ffd27a') * rng.uniform(0.7, 1.0)
                # Một vài toà có cột ăng-ten.
                if rng.random() < 0.15:
                    ax = int((x0 + bw / 2) * H)
                    img[int((y0 - 0.04) * H):yi0, max(ax - 1, 0):ax + 1] = hexc(col)
            x = x1 + rng.uniform(0.0, 0.01)

    # Mặt hồ: phản chiếu lật ngược, gợn sóng ngang và tối hơn.
    hz = int(horizon * H)
    rows = np.arange(hz, H)
    src = np.clip(2 * hz - rows, 0, hz - 1)
    ripple = (np.sin(rows * 0.9) * 4 + np.sin(rows * 0.23) * 3).astype(int)
    refl = np.empty((H - hz, W, 3), dtype=np.float32)
    for i, (r, sh) in enumerate(zip(src, ripple)):
        refl[i] = np.roll(img[r], sh, axis=0)
    water = ramp((Y[hz:] - horizon) / (1 - horizon), [(0, hexc('#241c4a')), (1, hexc('#06071a'))])
    fade = (0.55 * (1 - (rows - hz) / (H - hz)) ** 1.5)[:, None, None]
    img[hz:] = water * (1 - fade) + refl * fade
    # Vệt trăng trên mặt nước.
    trail = np.exp(-np.abs(X[hz:] - mx) / 0.03) * (np.sin(Y[hz:] * 900) > 0.2) * 0.25
    img[hz:] = img[hz:] + hexc('#fbefc4') * trail[..., None]
    img[hz:hz + 2] = img[hz:hz + 2] * 0.6
    return finish(img, grain=0.006, seed=22)


def main():
    out = Path(sys.argv[1] if len(sys.argv) > 1 else 'out')
    out.mkdir(parents=True, exist_ok=True)
    only = set(sys.argv[2:])
    groups = [silk_set, tidyo_set,
              lambda: {'hoat-hinh-nui-chang-vang': cartoon_mountains(),
                       'hoat-hinh-thanh-pho-dem': cartoon_city()}]
    for g in groups:
        for name, img in g().items():
            if only and name not in only:
                continue
            save_png(out / f'{name}.png', img)
            print('saved', name)


if __name__ == '__main__':
    main()
