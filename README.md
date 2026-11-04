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

## Architecture

The project follows the official Android Architecture guidelines with MVVM and the Repository pattern:

- **UI Layer**: Built with Jetpack Compose and Material 3. Screens observe `StateFlow` from ViewModels and render UI state. Search queries use debouncing to conserve network requests.
- **ViewModel Layer**: Manages screen state and pagination streams using `Flow<PagingData<Article>>` cached in `viewModelScope`.
- **Data Layer**:
  - `NewsRepository`: Single source of truth. Exposes paginated article streams with `Pager`.
  - `NewsRemoteMediator`: Coordinates network fetches from `NewsApi` and caching in Room. Detects the end of pagination early to minimize requests.
  - Room Database: Caches news articles and pagination remote keys. Tables use composite primary keys `(feedKey, url)` and `(feedKey, articleUrl)` to prevent collisions across feeds. Articles are ordered deterministically by an explicit `position` column.
- **Dependency Injection**: Manual DI via `AppContainer` initialized in `NewsApp`. This eliminates code generation and provides a clear dependency graph.
- **Article Viewer**: Full news stories open in Chrome Custom Tabs (`androidx.browser`).

## Known Limitations

- **NewsAPI Free Plan Limits**: The free plan allows up to 100 requests per day and 100 total results per query. The mediator halts pagination at 100 items to avoid HTTP 426 upgrade errors.
- **Country Filter**: Top headlines default to the United States (`"us"`). NewsAPI supports category filtering only on top headlines.
- **Search Scope**: Search returns articles matching the query from NewsAPI rather than querying across locally cached headline records.

## Use of AI

AI tools were used during development for:
- Researching NewsAPI specifications and Jetpack Compose Paging 3 integration.
- Evaluating architectural trade-offs (Room RemoteMediator vs network-only paging, manual DI vs Hilt).
- Reviewing code for edge cases and drafting test scenarios.

All architecture, database schemas, business logic, and tests were adapted, understood, and verified by the author.

## License

Licensed under the [GNU General Public License v3.0](LICENSE).
