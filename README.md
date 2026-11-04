# GuessWhat

Android news app built with Jetpack Compose and [NewsAPI](https://newsapi.org/).

## What is implemented

- **Home screen**: Top US headlines and a search bar with debounce.
- **Categories screen**: News filtered by category.
- **Pagination and local database**: Paging 3 with `RemoteMediator` and Room caching on all screens.
- **Article viewing**: Full articles open in Chrome Custom Tabs when tapping a card.
- **State handling**: Loading, empty, and error states with retry support.

## Setup and Build

1. Get a free API key at [newsapi.org/register](https://newsapi.org/register).
2. Add the key to `local.properties` in the project root:

```properties
NEWS_API_KEY=your_api_key_here
```

3. Build the project in Android Studio or from the terminal:

```bash
./gradlew assembleDebug
```

To run unit tests:

```bash
./gradlew testDebugUnitTest
```

## Tech Stack

| Area | Choice |
|---|---|
| UI | Jetpack Compose (Material 3) |
| Navigation | Navigation Compose (bottom bar, 2 tabs) |
| Networking | Retrofit + OkHttp |
| JSON | kotlinx.serialization |
| Asynchrony | Kotlin Coroutines + Flow |
| Local database | Room |
| Pagination | Paging 3 + RemoteMediator |
| Architecture | MVVM + Repository, state via StateFlow |
| DI | Manual (`AppContainer`) |
| Images | Coil 3 |
| Article viewing | Chrome Custom Tabs (`androidx.browser`) |

## Use of AI

AI was used during preparation for researching documentation, discussing trade-offs, and drafting boilerplate. All code in the repository was written, adapted, and understood by me to fit the chosen architecture and stack.

## License

Licensed under the [GNU General Public License v3.0](LICENSE).
