import Foundation
import ComposeApp

/**
 * Swift extensions for Kotlin StateFlow to enable observation in SwiftUI.
 *
 * Provides a `watch()` method that collects StateFlow emissions and calls the provided block.
 * This is used by ViewModelWrappers to bridge Kotlin StateFlow to SwiftUI @Published properties.
 */

// MARK: - FlowCollector

private class FlowCollector: Kotlinx_coroutines_coreFlowCollector {
    private let block: (Any?) -> Void

    init(block: @escaping (Any?) -> Void) {
        self.block = block
    }

    func emit(value: Any?, completionHandler: @escaping (Error?) -> Void) {
        block(value)
        completionHandler(nil)
    }
}

// MARK: - StateFlow Extension

extension Kotlinx_coroutines_coreStateFlow {
    /**
     * Observe StateFlow emissions from Swift code.
     *
     * - Parameter block: Closure called for each emission with the new value.
     *
     * Example usage:
     * ```swift
     * viewModel.uiState.watch { [weak self] uiState in
     *     guard let state = uiState as? MyUiState else { return }
     *     DispatchQueue.main.async {
     *         self?.someProperty = state.someValue
     *     }
     * }
     * ```
     */
    func watch(block: @escaping (Any?) -> Void) {
        let collector = FlowCollector(block: block)
        self.collect(collector: collector) { error in
            if let err = error {
                print("StateFlow collection error: \(String(describing: err))")
            }
        }
    }
}
