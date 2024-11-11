# NSD-Coroutines

This project provides a comprehensive solution for Network Service Discovery (NSD) in Android, including support for Android 14. The project leverages Kotlin Coroutines for efficient asynchronous programming and introduces a structured approach to discovering and resolving network services on local networks using `NsdManager`. Key features include coroutine-based service discovery, compatibility with the latest Android SDK changes, and multicast packet handling for local network communication.

## Features
- Network Service Discovery (NSD) support for Android, including Android 14 (SDK 34)
- Coroutine-based API for seamless asynchronous service discovery and resolution
- `MulticastLock` usage for multicast packet handling, enabling local network communication
- Adaptable service resolution using `ServiceInfoCallback` (Android 14+) and `ResolveListener` (pre-Android 14)
- Example usage of `ReceiveChannel` to discover and manage multiple services

## Installation
1. Clone this repository.
2. Open the project in Android Studio.
3. Build and run the project on an Android emulator or physical device.

## Usage
1. Integrate NSD logic with your project by including the helper functions in `NsdHelper`.
2. Use `discoverServices` to discover services on the local network by specifying the `serviceType` and `serviceName`.
3. Resolve services using `ServiceInfoCallback` or `ResolveListener` based on Android version compatibility.
4. Handle multicast communication with `MulticastLock` for reliable service discovery.
5. Example usage of the service discovery and resolution can be found in the `ExampleActivity`.

## Contributing
Contributions are welcome! Please feel free to submit pull requests or open issues for any bugs or feature requests.

## License
This project is licensed under the Apache-2.0 License - see the LICENSE file for details.
