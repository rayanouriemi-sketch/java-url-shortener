package fr.rayan.shortener;

final class HomePage {
    private HomePage() {
    }

    static final String HTML = """
            <!doctype html>
            <html lang="fr">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Raccourcisseur d'URL</title>
              <style>
                body { font-family: system-ui, sans-serif; max-width: 560px; margin: 4rem auto; padding: 0 1rem; }
                form { display: flex; gap: .5rem; }
                input { flex: 1; padding: .6rem; font-size: 1rem; }
                button { padding: .6rem 1rem; font-size: 1rem; cursor: pointer; }
                #result { margin-top: 1.5rem; font-size: 1.1rem; word-break: break-all; }
              </style>
            </head>
            <body>
              <h1>Raccourcisseur d'URL</h1>
              <form id="form">
                <label for="url" hidden>URL à raccourcir</label>
                <input id="url" type="url" placeholder="https://exemple.com/une/page/tres/longue" required>
                <button>Raccourcir</button>
              </form>
              <p id="result" role="status"></p>
              <script>
                document.getElementById('form').addEventListener('submit', async (e) => {
                  e.preventDefault();
                  const res = await fetch('/api/shorten', { method: 'POST', body: document.getElementById('url').value });
                  const text = await res.text();
                  const out = document.getElementById('result');
                  out.textContent = '';
                  if (res.ok) {
                    const a = document.createElement('a');
                    a.href = text;
                    a.textContent = text;
                    out.append(a);
                  } else {
                    out.textContent = 'Erreur : ' + text;
                  }
                });
              </script>
            </body>
            </html>
            """;
}
