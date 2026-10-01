# Aura news proxy

A tiny [Cloudflare Worker](https://developers.cloudflare.com/workers/) that keeps the
[NewsData.io](https://newsdata.io) API key on the server. The Android app never contains the key.

## Deploy

```bash
cd server/news-proxy
npx wrangler login
npx wrangler secret put NEWSDATA_API_KEY   # paste your NewsData.io key
npx wrangler deploy                         # prints https://aura-news-proxy.<you>.workers.dev
```

Then point the app at it (in the project's `local.properties`, or as an environment variable on CI):

```properties
aura.newsApiBaseUrl=https://aura-news-proxy.<you>.workers.dev/
```

Leave `aura.newsApiKey` empty. Rebuild the app.

The worker forwards only `country`, `category`, `size`, `page`, `removeduplicate` and `language`,
and caches responses for 10 minutes to save API credits. Any other host with the same
`/api/1/latest` contract (your own backend, a serverless function) works too.
