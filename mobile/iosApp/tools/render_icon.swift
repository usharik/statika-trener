// Vykreslí ikonu aplikace 1024×1024 ze stejné předlohy jako Android (app/src/main/res/drawable/ic_launcher_foreground.xml):
// nosník, kloubová a posuvná podpora, síla shora. Spuštění: swift tools/render_icon.swift Statika/Assets.xcassets/AppIcon.appiconset/AppIcon.png
import CoreGraphics
import ImageIO
import UniformTypeIdentifiers

let size = 1024.0
let out = CommandLine.arguments[1]
// RGB bez průhlednosti – App Store ikonu s alfa kanálem odmítne
let ctx = CGContext(data: nil, width: Int(size), height: Int(size), bitsPerComponent: 8, bytesPerRow: 0,
                    space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
func rgb(_ hex: Int) -> CGColor {
    CGColor(red: CGFloat((hex >> 16) & 255) / 255, green: CGFloat((hex >> 8) & 255) / 255, blue: CGFloat(hex & 255) / 255, alpha: 1)
}
ctx.setFillColor(rgb(0x1B2330))
ctx.fill(CGRect(x: 0, y: 0, width: size, height: size))
// souřadnice předlohy 0..32 (osa y dolů) → obrázek; iOS ikonu neořezává do kruhu, proto o něco větší než na Androidu
let scale = size / 32.0 * 0.78
let offX = (size - 32 * scale) / 2
let offY = (size - 32 * scale) / 2 + 3 * scale  // obsah předlohy leží v y 1..25 – vystředit
ctx.translateBy(x: 0, y: size)
ctx.scaleBy(x: 1, y: -1)
func p(_ x: Double, _ y: Double) -> CGPoint { CGPoint(x: offX + x * scale, y: offY + y * scale) }
// nosník se zaoblenými rohy
ctx.setFillColor(rgb(0xFFFFFF))
ctx.addPath(CGPath(roundedRect: CGRect(origin: p(3, 11), size: CGSize(width: 26 * scale, height: 4 * scale)), cornerWidth: scale, cornerHeight: scale, transform: nil))
ctx.fillPath()
// pevná kloubová podpora
ctx.move(to: p(7, 15)); ctx.addLine(to: p(3, 23)); ctx.addLine(to: p(11, 23)); ctx.closePath(); ctx.fillPath()
// posuvná podpora s čarou podložky
ctx.setFillColor(rgb(0x6F9FFF))
ctx.move(to: p(25, 15)); ctx.addLine(to: p(21, 22)); ctx.addLine(to: p(29, 22)); ctx.closePath(); ctx.fillPath()
ctx.setStrokeColor(rgb(0x6F9FFF)); ctx.setLineWidth(1.4 * scale)
ctx.move(to: p(20, 25)); ctx.addLine(to: p(30, 25)); ctx.strokePath()
// síla
ctx.setStrokeColor(rgb(0xFF7B5F)); ctx.setLineWidth(1.6 * scale)
ctx.move(to: p(16, 1)); ctx.addLine(to: p(16, 8)); ctx.strokePath()
ctx.setFillColor(rgb(0xFF7B5F))
ctx.move(to: p(16, 10.5)); ctx.addLine(to: p(13.6, 6.5)); ctx.addLine(to: p(18.4, 6.5)); ctx.closePath(); ctx.fillPath()
let dest = CGImageDestinationCreateWithURL(URL(fileURLWithPath: out) as CFURL, UTType.png.identifier as CFString, 1, nil)!
CGImageDestinationAddImage(dest, ctx.makeImage()!, nil)
CGImageDestinationFinalize(dest)
