# CHANGELOG
All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). See the [CONTRIBUTING guide](./CONTRIBUTING.md#Changelog) for instructions on how to add changelog entries.

## [Unreleased 2.x]
### Added
- Add security policy to allow `accessUnixDomainSocket` in `transport-grpc` module ([#20463](https://github.com/opensearch-project/OpenSearch/pull/20463))

### Changed

### Fixed
- Fix flaky test failures in ShardsLimitAllocationDeciderIT ([#20375](https://github.com/opensearch-project/OpenSearch/pull/20375))
- Prevent criteria update for context aware indices ([#20250](https://github.com/opensearch-project/OpenSearch/pull/20250))
- Update EncryptedBlobContainer to adhere limits while listing blobs in specific sort order if wrapped blob container supports ([#20514](https://github.com/opensearch-project/OpenSearch/pull/20514))

### Dependencies

### Deprecated

### Removed

### Fixed
- Fix ShardSearchFailure in transport-grpc ([#20641](https://github.com/opensearch-project/OpenSearch/pull/20641))

### Security

### Changed
