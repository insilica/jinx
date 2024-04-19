## Unreleased

## v0.3.0 (2024-04-19)

- Upgrade dependencies

## v0.2.0 (2022-06-23)
- Forked from [https://github.com/juxt/jinx](juxt/jinx)
- Support compiling in babashka by excluding `idn-hostname?`` feature when run with babashka
- Fix ClojureScript validation causing an `Error`` instead of returning a validation feature [juxt#26](https://github.com/juxt/jinx/pull/26)
- Fix schema with "uniqueItems": true would fail validation on an empty array [juxt#25](https://github.com/juxt/jinx/pull/25)
