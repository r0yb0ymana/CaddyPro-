import XCTest
@testable import App

final class ExampleTests: XCTestCase {
    
    func testExample() throws {
        // Given
        let value = 2 + 2
        
        // Then
        XCTAssertEqual(value, 4)
    }
    
    func testUserModel() throws {
        // Given
        let user = User(
            id: "123",
            email: "test@example.com",
            name: "Test User",
            createdAt: Date()
        )
        
        // Then
        XCTAssertEqual(user.id, "123")
        XCTAssertEqual(user.email, "test@example.com")
        XCTAssertEqual(user.name, "Test User")
    }
    
    func testUserEquatable() throws {
        // Given
        let date = Date()
        let user1 = User(id: "123", email: "test@example.com", name: "Test", createdAt: date)
        let user2 = User(id: "123", email: "test@example.com", name: "Test", createdAt: date)
        
        // Then
        XCTAssertEqual(user1, user2)
    }
}
