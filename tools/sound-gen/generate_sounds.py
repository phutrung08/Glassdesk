"""Tạo vài âm thanh điều hướng nhẹ nhàng cho Glassdeck, chỉ dùng numpy và thư viện chuẩn.

Mọi âm thanh đều được tổng hợp bằng thuật toán trong file này, nên thuộc quyền của dự án
(MIT, xem LICENSE) và không phụ thuộc kho âm thanh nào bên ngoài. Chạy:

    python -I generate_sounds.py <thư_mục_ra>

Kết quả là các file WAV 16 bit, mono, 44,1 kHz, đỉnh khoảng -10 dBFS (cố ý nhỏ, êm tai).
"""
import sys
import wave
from pathlib import Path

import numpy as np

SR = 44100
PEAK = 0.32  # ~ -10 dBFS: nhẹ hơn nhiều so với tiếng "tách" mặc định của hệ thống


def t_axis(ms):
    return np.arange(int(SR * ms / 1000)) / SR


def envelope(t, attack_ms, decay_ms):
    """Vào nhanh, tắt dần theo hàm mũ, và tắt hẳn ở cuối để không có tiếng 'bụp'."""
    a = np.clip(t / (attack_ms / 1000), 0, 1) ** 2
    d = np.exp(-t / (decay_ms / 1000))
    tail = np.clip((t[-1] - t) / 0.008, 0, 1)
    return a * d * tail


def finish(x):
    x = x / (np.max(np.abs(x)) + 1e-9) * PEAK
    return (x * 32767).astype(np.int16)


def lowpass(x, cutoff_hz):
    """Bộ lọc một cực đủ cho việc làm tròn tiếng ồn."""
    rc = 1.0 / (2 * np.pi * cutoff_hz)
    alpha = (1.0 / SR) / (rc + 1.0 / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc += alpha * (v - acc)
        y[i] = acc
    return y


def drop():
    """Giọt nước: tiếng sin trượt nhẹ từ cao xuống thấp."""
    t = t_axis(110)
    f = 880 * np.exp(-t * 9) + 420
    phase = 2 * np.pi * np.cumsum(f) / SR
    return finish(np.sin(phase) * envelope(t, 4, 28))


def wood():
    """Gõ gỗ nhẹ: hai hoạ âm thấp tắt nhanh, thêm chút tiếng chạm rất ngắn."""
    t = t_axis(120)
    rng = np.random.default_rng(1)
    body = np.sin(2 * np.pi * 360 * t) + 0.35 * np.sin(2 * np.pi * 720 * t) + 0.12 * np.sin(2 * np.pi * 1180 * t)
    touch = lowpass(rng.normal(0, 1, t.size), 1800) * np.exp(-t / 0.004) * 0.5
    return finish((body * envelope(t, 2, 22)) + touch)


def chime():
    """Chuông nhỏ: một nốt cao mềm, ngân ngắn."""
    t = t_axis(260)
    x = np.sin(2 * np.pi * 1318 * t) + 0.28 * np.sin(2 * np.pi * 2637 * t) + 0.06 * np.sin(2 * np.pi * 3951 * t)
    return finish(x * envelope(t, 6, 70))


def soft_tick():
    """Tích mờ: rất ngắn và trầm, gần như chỉ là một cái chạm."""
    t = t_axis(60)
    rng = np.random.default_rng(2)
    noise = lowpass(rng.normal(0, 1, t.size), 900) * np.exp(-t / 0.008)
    tone = np.sin(2 * np.pi * 520 * t) * envelope(t, 2, 10)
    return finish(noise * 0.9 + tone * 0.8)


def save(path, samples):
    with wave.open(str(path), 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(samples.tobytes())


def main():
    out = Path(sys.argv[1] if len(sys.argv) > 1 else 'sounds')
    out.mkdir(parents=True, exist_ok=True)
    for name, fn in (('1-giot-nuoc', drop), ('2-go-go-nhe', wood), ('3-chuong-nho', chime), ('4-tich-mo', soft_tick)):
        samples = fn()
        save(out / f'{name}.wav', samples)
        print(f'{name}.wav  {len(samples) / SR * 1000:.0f} ms')


if __name__ == '__main__':
    main()
