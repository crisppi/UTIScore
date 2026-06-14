import AppKit

let size = 1024
let output = CommandLine.arguments.count > 1
  ? CommandLine.arguments[1]
  : "app-store-assets/uti-score-icon-1024.png"

let image = NSImage(size: NSSize(width: size, height: size))
image.lockFocus()

let rect = NSRect(x: 0, y: 0, width: size, height: size)
NSColor(calibratedRed: 0.02, green: 0.12, blue: 0.22, alpha: 1).setFill()
rect.fill()

let gradient = NSGradient(colors: [
  NSColor(calibratedRed: 0.02, green: 0.13, blue: 0.25, alpha: 1),
  NSColor(calibratedRed: 0.02, green: 0.29, blue: 0.42, alpha: 1),
  NSColor(calibratedRed: 0.04, green: 0.56, blue: 0.60, alpha: 1),
])!
gradient.draw(in: rect, angle: 315)

let inset: CGFloat = 122
let panel = NSBezierPath(
  roundedRect: NSRect(x: inset, y: inset, width: CGFloat(size) - inset * 2, height: CGFloat(size) - inset * 2),
  xRadius: 96,
  yRadius: 96
)
NSColor(calibratedWhite: 1, alpha: 0.10).setFill()
panel.fill()
NSColor(calibratedWhite: 1, alpha: 0.22).setStroke()
panel.lineWidth = 5
panel.stroke()

let accent = NSBezierPath(
  roundedRect: NSRect(x: 300, y: 706, width: 424, height: 28),
  xRadius: 14,
  yRadius: 14
)
NSColor(calibratedRed: 0.35, green: 0.93, blue: 0.78, alpha: 1).setFill()
accent.fill()

let paragraph = NSMutableParagraphStyle()
paragraph.alignment = .center

let title = "UTI"
let font = NSFont.systemFont(ofSize: 250, weight: .black)
let attributes: [NSAttributedString.Key: Any] = [
  .font: font,
  .foregroundColor: NSColor.white,
  .paragraphStyle: paragraph,
]
title.draw(in: NSRect(x: 84, y: 366, width: 856, height: 280), withAttributes: attributes)

let subtitle = "SCORE"
let subtitleFont = NSFont.systemFont(ofSize: 58, weight: .semibold)
let subtitleAttributes: [NSAttributedString.Key: Any] = [
  .font: subtitleFont,
  .foregroundColor: NSColor(calibratedWhite: 1, alpha: 0.88),
  .paragraphStyle: paragraph,
]
subtitle.draw(in: NSRect(x: 120, y: 306, width: 784, height: 76), withAttributes: subtitleAttributes)

image.unlockFocus()

guard
  let tiff = image.tiffRepresentation,
  let bitmap = NSBitmapImageRep(data: tiff),
  let png = bitmap.representation(using: .png, properties: [:])
else {
  fatalError("Nao foi possivel gerar o PNG do icone.")
}

try png.write(to: URL(fileURLWithPath: output))
print(output)
