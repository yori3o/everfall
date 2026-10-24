"""Build a GIF from vertical particle sheets. Requires Python 3.10+ and Pillow.

Examples:
    python tools/make_leaf_gif.py
    python tools/make_leaf_gif.py --fps 12
    python tools/make_leaf_gif.py --frame-duration 0.0833333333
    python tools/make_leaf_gif.py --scale 30 --x 100 --y 120 --output artifacts/custom.gif

Edit DEFAULTS below or override settings with command-line arguments.
The complete normal animation is followed by the complete alternative animation.
"""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import GifImagePlugin, Image, ImageChops, ImageFilter


PROJECT_ROOT = Path(__file__).resolve().parents[1]
TEXTURE_DIR = PROJECT_ROOT / "src/main/resources/assets/everfall/textures/particle/autumn"

DEFAULTS = {
    "normal": TEXTURE_DIR / "leaf_2.png",
    "alternative": TEXTURE_DIR / "leaf_2_alternative.png",
    "background": Path.home() / "Desktop/bg.png",
    "output": PROJECT_ROOT / "artifacts/leaf_animation.gif",
    # None saves a PNG next to the GIF, with the same filename stem.
    "first_frame": None,
    "frame_size": 8,
    "frame_count": 8,
    "scale": 33.75,
    "mirror": True,
    # Store only the changed rectangle after the first frame, without losing colors.
    "optimize_frames": True,
    "fps": 5.0,
    # Set to 1 / 12 for that duration instead of the FPS setting.
    "frame_duration": None,
    # None centers the sprite; otherwise use background pixel coordinates.
    "x": None,
    "y": None,
    # Offset from the centered or explicitly specified Y coordinate, in output pixels.
    "offset_y": -34,
    "sharpen_radius": 3.0,
    "sharpen_percent": 35,
    "sharpen_threshold": 3,
    "shadow_opacity": 0.22,
    "shadow_blur": 5.0,
    "shadow_x": 3,
    "shadow_y": 5,
    # GIF: 0 means repeat forever.
    "loop": 0,
    # Used only if the background PNG has transparent pixels.
    "matte": "#ffffff",
}


def read_frames(path: Path, frame_size: int, frame_count: int) -> list[Image.Image]:
    with Image.open(path) as source:
        sheet = source.convert("RGBA")
    expected = (frame_size, frame_size * frame_count)
    if sheet.size != expected:
        raise ValueError(f"{path}: expected a {expected} vertical sheet, got {sheet.size}")
    return [
        sheet.crop((0, index * frame_size, frame_size, (index + 1) * frame_size))
        for index in range(frame_count)
    ]


def gif_durations(frame_seconds: float, count: int) -> list[int]:
    """GIF stores time in 10 ms units; distribute rounding across the cycle."""
    if frame_seconds < 0.02:
        raise ValueError("Use a duration of at least 0.02 seconds (at most 50 FPS)")
    boundaries = [round(index * frame_seconds * 100) for index in range(count + 1)]
    return [(end - start) * 10 for start, end in zip(boundaries, boundaries[1:])]


def create_gif(args: argparse.Namespace) -> tuple[int, tuple[int, int], list[int]]:
    if args.frame_size <= 0 or args.frame_count <= 0 or args.scale <= 0:
        raise ValueError("Frame size, frame count and scale must be positive")
    if args.loop < 0:
        raise ValueError("Loop count must be nonnegative")
    if not 0 <= args.shadow_opacity <= 1 or args.shadow_blur < 0:
        raise ValueError("Shadow opacity must be between 0 and 1, and blur must be nonnegative")
    if args.sharpen_radius < 0 or args.sharpen_percent < 0 or not 0 <= args.sharpen_threshold <= 255:
        raise ValueError("Sharpen radius/percent must be nonnegative, and threshold must be between 0 and 255")
    seconds = args.frame_duration
    if seconds is None:
        if args.fps <= 0:
            raise ValueError("FPS must be positive")
        seconds = 1 / args.fps

    with Image.open(args.background) as source:
        background = source.convert("RGBA")
    matte = Image.new("RGBA", background.size, args.matte)
    matte.alpha_composite(background)
    background = matte

    frames = read_frames(args.normal, args.frame_size, args.frame_count)
    frames += read_frames(args.alternative, args.frame_size, args.frame_count)
    side = max(1, round(args.frame_size * args.scale))
    x = (background.width - side) // 2 if args.x is None else args.x
    y = (background.height - side) // 2 if args.y is None else args.y
    y += args.offset_y

    rendered = []
    for sprite in frames:
        if args.mirror:
            sprite = sprite.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
        # Nearest-neighbor keeps the pixel-art edges sharp at fractional scales.
        sprite = sprite.resize((side, side), Image.Resampling.NEAREST)
        canvas = background.copy()
        # Blur the actual silhouette on a full-size layer so the shadow can extend
        # beyond the sprite's rectangle without producing a rectangular backdrop.
        if args.shadow_opacity > 0:
            mask = Image.new("L", background.size, 0)
            mask.paste(sprite.getchannel("A"), (x + args.shadow_x, y + args.shadow_y))
            mask = mask.filter(ImageFilter.GaussianBlur(args.shadow_blur))
            mask = mask.point([round(alpha * args.shadow_opacity) for alpha in range(256)])
            shadow = Image.new("RGBA", background.size, (0, 0, 0, 0))
            shadow.putalpha(mask)
            canvas.alpha_composite(shadow)
        canvas.alpha_composite(sprite, (x, y))
        frame = canvas.convert("RGB")
        if args.sharpen_percent > 0:
            frame = frame.filter(ImageFilter.UnsharpMask(
                radius=args.sharpen_radius, percent=args.sharpen_percent, threshold=args.sharpen_threshold))
        rendered.append(frame)

    # Preserve the first composed frame at full PNG quality, before GIF quantization.
    first_frame_path = args.first_frame or args.output.with_suffix(".png")
    first_frame_path.parent.mkdir(parents=True, exist_ok=True)
    rendered[0].save(first_frame_path, format="PNG")

    # One palette for the whole animation prevents color shifts in the static background.
    palette_source = Image.new("RGB", (background.width, background.height * len(rendered)))
    for index, frame in enumerate(rendered):
        palette_source.paste(frame, (0, index * background.height))
    palette = palette_source.quantize(colors=256, method=Image.Quantize.MEDIANCUT)
    indexed = [frame.quantize(palette=palette, dither=Image.Dither.NONE) for frame in rendered]
    durations = gif_durations(seconds, len(indexed))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    # Write every frame explicitly: Pillow's multi-frame save otherwise merges
    # identical adjacent source frames, even with optimize=False.
    with args.output.open("wb") as output:
        header, _ = GifImagePlugin.getheader(indexed[0], info={"loop": args.loop, "optimize": False})
        for block in header:
            output.write(block)
        previous = None
        for frame, duration in zip(indexed, durations):
            offset = (0, 0)
            encoded_frame = frame
            if args.optimize_frames and previous is not None:
                bounds = ImageChops.difference(frame.convert("RGB"), previous.convert("RGB")).getbbox()
                # An unchanged 1x1 pixel preserves duplicate frames and their timing.
                bounds = bounds or (0, 0, 1, 1)
                encoded_frame = frame.crop(bounds)
                offset = bounds[:2]
            for block in GifImagePlugin.getdata(encoded_frame, offset=offset, duration=duration, disposal=1):
                output.write(block)
            previous = frame
        output.write(b";")
    return len(indexed), (side, side), durations


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    for name in ("normal", "alternative", "background", "output", "first_frame"):
        parser.add_argument(f"--{name.replace('_', '-')}", type=Path, default=DEFAULTS[name])
    parser.add_argument("--frame-size", type=int, default=DEFAULTS["frame_size"])
    parser.add_argument("--frame-count", type=int, default=DEFAULTS["frame_count"])
    parser.add_argument("--scale", type=float, default=DEFAULTS["scale"])
    parser.add_argument("--mirror", action=argparse.BooleanOptionalAction, default=DEFAULTS["mirror"],
                        help="Mirror the sprite horizontally; --no-mirror disables it")
    parser.add_argument("--optimize-frames", action=argparse.BooleanOptionalAction,
                        default=DEFAULTS["optimize_frames"], help="Losslessly encode only changed frame rectangles")
    timing = parser.add_mutually_exclusive_group()
    timing.add_argument("--fps", type=float)
    timing.add_argument("--frame-duration", type=float, help="Seconds per frame; e.g. 0.0833333333 for 1/12 s")
    parser.add_argument("--x", type=int, default=DEFAULTS["x"])
    parser.add_argument("--y", type=int, default=DEFAULTS["y"])
    parser.add_argument("--offset-y", type=int, default=DEFAULTS["offset_y"],
                        help="Vertical offset in output pixels; negative moves up")
    parser.add_argument("--sharpen-radius", type=float, default=DEFAULTS["sharpen_radius"])
    parser.add_argument("--sharpen-percent", type=int, default=DEFAULTS["sharpen_percent"],
                        help="Unsharp-mask strength; 0 disables sharpening")
    parser.add_argument("--sharpen-threshold", type=int, default=DEFAULTS["sharpen_threshold"])
    parser.add_argument("--shadow-opacity", type=float, default=DEFAULTS["shadow_opacity"],
                        help="Shadow opacity from 0 to 1; 0 disables the shadow")
    parser.add_argument("--shadow-blur", type=float, default=DEFAULTS["shadow_blur"])
    parser.add_argument("--shadow-x", type=int, default=DEFAULTS["shadow_x"])
    parser.add_argument("--shadow-y", type=int, default=DEFAULTS["shadow_y"])
    parser.add_argument("--loop", type=int, default=DEFAULTS["loop"])
    parser.add_argument("--matte", default=DEFAULTS["matte"])
    args = parser.parse_args()
    if args.fps is None and args.frame_duration is None:
        args.frame_duration = DEFAULTS["frame_duration"]
        args.fps = DEFAULTS["fps"]
    return args


if __name__ == "__main__":
    count, sprite_size, durations = create_gif(parse_args())
    print(f"Created {count} frames; sprite {sprite_size[0]}x{sprite_size[1]}; cycle {sum(durations)} ms")
