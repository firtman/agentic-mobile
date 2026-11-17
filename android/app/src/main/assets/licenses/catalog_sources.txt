# Catalog source attribution

The local setup downloads a normalized catalog from https://movie-api-delta-six.vercel.app/catalog. The starter does not redistribute the dataset. The external catalog combines these sources:

- [prust/wikipedia-movie-data](https://github.com/prust/wikipedia-movie-data): titles, release years, cast, genres, Wikipedia links, extracts and source poster URLs. Its code license does not replace Wikipedia content terms. Wikipedia-derived text is [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/); see [reuse requirements](https://en.wikipedia.org/wiki/Wikipedia:Reusing_Wikipedia_content). Preserve each record's `sourceUrl` for attribution, authors, and history. Derived plot-clue trivia retains the underlying text's terms.
- [CMU Movie Summary Corpus](https://www.cs.cmu.edu/~ark/personas/): Wikipedia IDs and runtime metadata, matched by title and year. Distributed under Creative Commons Attribution-ShareAlike. Cite David Bamman, Brendan O'Connor, and Noah Smith, “Learning Latent Personas of Film Characters,” ACL 2013. Check the corpus's own terms before redistribution.
- [9,000+ Movies Dataset by dish am993](https://www.kaggle.com/datasets/disham993/9000-movies-dataset): additional catalog records and supplied poster URLs, source vote averages/counts, popularity, release dates, and language. Preserve `datasetSourceUrl` where supplied. Consult the dataset's current terms before redistribution; access to a URL does not establish ownership of artwork.

The source project's original Wikipedia/CMU snapshot and Kaggle archive were retrieved November 16, 2026. The hosted catalog can change. Movie metadata is in its source language. Zero ratings/runtime and blank credits represent missing values; do not invent replacements. Source vote averages are distinct from personal preferences. Images remain subject to their respective rights and source terms.

If you incorporate downloaded data into an app, preserve attribution links and applicable ShareAlike notices in the app and distribution. Record any source changes. This starter includes neither raw archives nor catalog records nor poster images.
