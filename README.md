# Raccourcisseur d'URL en Java

Un service web qui transforme une URL longue en lien court (comme bit.ly), sans framework : uniquement le serveur HTTP intégré au JDK.

## Fonctionnalités

- Page web pour raccourcir un lien
- API REST :

| Méthode | Route | Description |
|---------|-------|-------------|
| POST | /api/shorten | corps = URL, retourne le lien court (201) |
| GET | /{code} | redirection 302 vers l'URL d'origine |
| GET | /api/stats/{code} | nombre de clics (JSON) |

- Codes courts en **base 62** générés à partir d'un compteur atomique (aucune collision possible)
- Une même URL donne toujours le même code
- Validation : seules les URL `http`/`https` avec un domaine sont acceptées (pas de `javascript:`), taille limitée
- Serveur multithread, structures concurrentes (`ConcurrentHashMap`, `AtomicLong`)

## Lancer

```bash
mvn package
java -jar target/url-shortener-1.0.0.jar 8080
```

Puis ouvrir http://localhost:8080, ou en ligne de commande :

```bash
curl -X POST --data "https://fr.wikipedia.org/wiki/Java_(langage)" http://localhost:8080/api/shorten
```

## Tests

```bash
mvn test
```

Les tests démarrent le serveur sur un port libre et l'interrogent avec `java.net.http.HttpClient`.

## Limites

Les liens sont stockés en mémoire : ils disparaissent au redémarrage. Pas de limitation de débit : à ne pas exposer tel quel sur Internet.
