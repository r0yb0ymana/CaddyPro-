// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "App",
    platforms: [
        .iOS(.v17),
        .macOS(.v14)
    ],
    products: [
        .library(
            name: "App",
            targets: ["App"]
        ),
    ],
    dependencies: [
        // Add external dependencies here
        // .package(url: "https://github.com/example/package.git", from: "1.0.0"),
    ],
    targets: [
        .target(
            name: "App",
            dependencies: [],
            path: "Sources"
        ),
        .testTarget(
            name: "AppTests",
            dependencies: ["App"],
            path: "Tests/AppTests"
        ),
    ]
)
