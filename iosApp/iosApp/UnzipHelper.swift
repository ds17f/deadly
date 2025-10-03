import Foundation
import ZIPFoundation

@objc public class UnzipHelper: NSObject {

    /// Unzip a file from `sourceURL` into `destinationURL`.
    /// - Parameters:
    ///   - sourceURL: The ZIP file location.
    ///   - destinationURL: The folder to extract into.
    ///   - overwriteExisting: Whether to overwrite existing files.
    /// - Returns: The destination URL.
    @objc public static func unzipFile(
        at sourceURL: NSURL,
        to destinationURL: NSURL,
        overwriteExisting: Bool
    ) throws -> NSURL {

        let fileManager = FileManager.default
        let src = sourceURL as URL
        let dest = destinationURL as URL

        // Create destination directory
        try fileManager.createDirectory(at: dest, withIntermediateDirectories: true, attributes: nil)

        // Handle overwrite by clearing destination contents
        if overwriteExisting && fileManager.fileExists(atPath: dest.path) {
            let contents = try fileManager.contentsOfDirectory(atPath: dest.path)
            for item in contents {
                try fileManager.removeItem(atPath: dest.appendingPathComponent(item).path)
            }
        }

        // Use ZIPFoundation's FileManager extension
        try fileManager.unzipItem(at: src, to: dest)

        return dest as NSURL
    }
}
