# Workflow — Système P2P de Réplication de Fichiers

## Vue d'ensemble

```
┌─────────────────────────────────────────────────────────┐
│                    RÉSEAU P2P                            │
│                                                         │
│   ┌──────────────┐         ┌──────────────┐             │
│   │   Nœud A     │◄───────►│   Nœud B     │             │
│   │  port 5000   │         │  port 5001   │             │
│   └──────┬───────┘         └──────┬───────┘             │
│          │                        │                     │
│          └───────────┬────────────┘                     │
│                      │                                  │
│               ┌──────▼───────┐                          │
│               │   Nœud C     │                          │
│               │  port 5002   │                          │
│               └──────────────┘                          │
└─────────────────────────────────────────────────────────┘
```

Chaque nœud est une **instance indépendante** de la même application Spring Boot.
Ils communiquent directement entre eux via HTTP — **aucun serveur central**.

---

## Structure du projet

```
file_replicationP2P/
├── src/main/java/.../
│   ├── ProjetPtoPApplication.java       ← point d'entrée Spring Boot
│   ├── config/
│   │   ├── NodeConfig.java              ← lit storage + peers depuis application.yml
│   │   └── AppConfig.java               ← bean RestTemplate (appels HTTP inter-nœuds)
│   ├── controller/
│   │   ├── FileController.java          ← endpoints fichiers (upload/download/list)
│   │   └── NodeController.java          ← endpoint /node/info
│   └── service/
│       └── FileService.java             ← toute la logique P2P
├── src/main/resources/
│   ├── application.yml                  ← config par défaut (port 5000)
│   ├── application-node-a.yml           ← profil nœud A (port 5000)
│   ├── application-node-b.yml           ← profil nœud B (port 5001)
│   └── application-node-c.yml           ← profil nœud C (port 5002)
├── launch-nodes.sh                      ← script de lancement des 3 nœuds
└── pom.xml                              ← dépendances Maven
```

---

## Configuration d'un nœud

Chaque nœud est configuré via son fichier `application-node-X.yml` :

```yaml
server:
  port: 5000                        # port d'écoute

node:
  storage: storage_node_5000        # dossier de stockage local
  peers:
    - http://localhost:5001         # adresses des autres nœuds connus
    - http://localhost:5002
```

Ces valeurs sont lues par `NodeConfig.java` grâce à `@ConfigurationProperties(prefix = "node")`.

---

## Démarrage

### Lancer les 3 nœuds automatiquement

```bash
./launch-nodes.sh
```

### Lancer manuellement (un terminal par nœud)

```bash
# Terminal 1
./mvnw spring-boot:run -Dspring-boot.run.profiles=node-a

# Terminal 2
./mvnw spring-boot:run -Dspring-boot.run.profiles=node-b

# Terminal 3
./mvnw spring-boot:run -Dspring-boot.run.profiles=node-c
```

> Le terminal doit rester ouvert — Spring Boot tourne en continu.

### Vérifier que les nœuds tournent

```bash
curl http://localhost:5000/node/info
curl http://localhost:5001/node/info
curl http://localhost:5002/node/info
```

### Arrêter tous les nœuds

```bash
kill $(lsof -ti:5000,5001,5002)
```

---

## Fonctionnement détaillé

### 1. Upload d'un fichier

**Endpoint :** `POST /files/{filename}`

```
Client
  │
  └─► POST /files/rapport.pdf → Nœud A (5000)
            │
            ├─ 1. FileService.saveFile()
            │       └─ saveLocalOnly() → écrit dans storage_node_5000/rapport.pdf
            │
            └─ 2. FileService.replicateFile()
                    ├─ POST /files/replicate/rapport.pdf → Nœud B (5001)
                    │       └─ saveLocalOnly() → storage_node_5001/rapport.pdf
                    └─ POST /files/replicate/rapport.pdf → Nœud C (5002)
                            └─ saveLocalOnly() → storage_node_5002/rapport.pdf
```

L'endpoint `/files/replicate/{filename}` est **réservé aux nœuds** — il sauvegarde sans re-répliquer pour éviter la boucle infinie.

---

### 2. Download d'un fichier

**Endpoint :** `GET /files/{filename}`

```
Client
  │
  └─► GET /files/rapport.pdf → Nœud A (5000)
            │
            ├─ 1. Cherche dans storage_node_5000/
            │       ├─ Trouvé → retourne le fichier ✓
            │       └─ Absent → passe à l'étape 2
            │
            └─ 2. FileService.searchInPeers()
                    ├─ GET /files/rapport.pdf → Nœud B (5001)
                    │       ├─ Trouvé → retourne le fichier ✓
                    │       └─ Absent ou injoignable → essaie le suivant
                    └─ GET /files/rapport.pdf → Nœud C (5002)
                            ├─ Trouvé → retourne le fichier ✓
                            └─ Absent → retourne 404
```

---

### 3. Tolérance aux pannes

```
Situation : Nœud A est arrêté

Client
  │
  └─► GET /files/rapport.pdf → Nœud B (5001)
            │
            ├─ Cherche localement → Trouvé (grâce à la réplication)
            └─ Retourne le fichier ✓  →  Pas besoin de Nœud A
```

Si le fichier n'est pas en local sur B :

```
Nœud B
  │
  └─ searchInPeers()
        ├─ GET → Nœud A (5000) → ECHEC (nœud arrêté) → warn log, continue
        └─ GET → Nœud C (5002) → Trouvé → retourne le fichier ✓
```

Le `try/catch` autour de chaque appel peer garantit que la panne d'un nœud n'arrête pas le système.

---

### 4. Liste des fichiers

**Endpoint :** `GET /files`

Retourne la liste des fichiers dans le dossier local du nœud interrogé :

```bash
curl http://localhost:5000/files
# ["rapport.pdf", "photo.jpg"]

curl http://localhost:5001/files
# ["rapport.pdf", "photo.jpg"]   ← répliqués automatiquement
```

---

## Endpoints complets

| Méthode | URL | Rôle | Appelé par |
|---|---|---|---|
| `POST` | `/files/{filename}` | Upload + réplication | Client |
| `GET` | `/files/{filename}` | Download (local → peers) | Client |
| `GET` | `/files` | Liste fichiers locaux | Client |
| `GET` | `/node/info` | Infos du nœud | Client / Debug |
| `POST` | `/files/replicate/{filename}` | Réplication sans re-répliquer | Nœuds uniquement |

---

## Logs

Chaque opération génère un log préfixé pour faciliter le suivi :

| Préfixe | Signification |
|---|---|
| `[LOCAL]` | Opération sur le stockage local |
| `[REPLICATION]` | Envoi du fichier vers un peer |
| `[SEARCH]` | Recherche du fichier sur les peers |

Exemple de logs lors d'un upload sur le nœud A :

```
[LOCAL]       Fichier 'test.txt' sauvegardé (26 octets).
[REPLICATION] Fichier 'test.txt' répliqué vers http://localhost:5001.
[REPLICATION] Fichier 'test.txt' répliqué vers http://localhost:5002.
```

Exemple lors d'un download d'un fichier absent localement :

```
[LOCAL]  Fichier 'test.txt' absent, recherche sur les peers...
[SEARCH] Fichier 'test.txt' trouvé sur http://localhost:5001.
```

---

## Scénarios de test

### Scénario 1 — Réplication automatique

```bash
# Upload sur A
curl -X POST http://localhost:5000/files/test.txt \
  -H "Content-Type: application/octet-stream" \
  --data-binary "Hello P2P"

# Vérifier sur B et C
curl http://localhost:5001/files/test.txt
curl http://localhost:5002/files/test.txt
```

### Scénario 2 — Recherche distribuée

```bash
# Upload uniquement sur B (sans réplication)
curl -X POST http://localhost:5001/files/replicate/secret.txt \
  -H "Content-Type: application/octet-stream" \
  --data-binary "Fichier secret"

# Télécharger depuis A → A cherche sur ses peers et trouve sur B
curl http://localhost:5000/files/secret.txt
```

### Scénario 3 — Tolérance aux pannes

```bash
# 1. Upload sur A (répliqué sur B et C)
curl -X POST http://localhost:5000/files/important.txt \
  -H "Content-Type: application/octet-stream" \
  --data-binary "Données critiques"

# 2. Arrêter le nœud A
kill $(lsof -ti:5000)

# 3. Fichier toujours accessible via B
curl http://localhost:5001/files/important.txt
```

---

## Classes clés

### FileService.java

| Méthode | Visibilité | Rôle |
|---|---|---|
| `saveFile()` | public | Sauvegarde locale + appel réplication |
| `saveLocalOnly()` | public | Sauvegarde locale uniquement (anti-boucle) |
| `getFile()` | public | Lecture locale + fallback peers |
| `listFiles()` | public | Liste les fichiers du dossier local |
| `replicateFile()` | private | POST vers chaque peer |
| `searchInPeers()` | private | GET sur chaque peer jusqu'à trouver |

### NodeConfig.java

Lit automatiquement la configuration YAML :
- `node.storage` → chemin du dossier de stockage
- `node.peers` → liste des URLs des peers connus
