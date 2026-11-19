from pathlib import Path

from PIL import Image, ImageDraw

BLUE = (9, 105, 218, 255)
WHITE = (255, 255, 255, 255)
CANVAS = 512
SCALE = 8
HEXAGON = [(256, 36), (446.5, 146), (446.5, 366), (256, 476), (65.5, 366), (65.5, 146)]
HEXAGON_STROKE = 24
LETTER = [(176, 380), (256, 140), (336, 380)]
LETTER_STROKE = 38
BAR = [(208, 316), (304, 316)]
BAR_STROKE = 30
ICON_SIZES = [16, 24, 32, 48, 64, 128, 256, 512, 1024]


def scaled(points):
    return [(x * SCALE, y * SCALE) for x, y in points]


def rounded_line(draw, points, width):
    path = scaled(points)
    thickness = width * SCALE
    draw.line(path, fill=WHITE, width=thickness, joint="curve")

    for x, y in (path[0], path[-1]):
        radius = thickness / 2
        draw.ellipse([x - radius, y - radius, x + radius, y + radius], fill=WHITE)


def render(size):
    side = CANVAS * SCALE
    image = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    hexagon = scaled(HEXAGON)

    draw.polygon(hexagon, fill=BLUE)
    draw.line(hexagon + [hexagon[0], hexagon[1]], fill=BLUE, width=HEXAGON_STROKE * SCALE, joint="curve")
    rounded_line(draw, LETTER, LETTER_STROKE)
    rounded_line(draw, BAR, BAR_STROKE)

    return image.resize((size, size), Image.LANCZOS)


def main(output, window_icon):
    folder = Path(output)
    folder.mkdir(parents=True, exist_ok=True)
    master = render(1024)
    window = master.resize((512, 512), Image.LANCZOS)

    window.save(folder / "icon.png")
    window.save(window_icon)
    master.save(folder / "icon.icns", format="ICNS", sizes=[(s, s) for s in (16, 32, 64, 128, 256, 512, 1024)])
    master.save(folder / "icon.ico", format="ICO", sizes=[(s, s) for s in (16, 24, 32, 48, 64, 128, 256)])


if __name__ == "__main__":
    main("archwright-desktop/packaging",
         "archwright-desktop/src/main/resources/io/github/arthursouto/archwright/desktop/icon.png")
