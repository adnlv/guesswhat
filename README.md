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
