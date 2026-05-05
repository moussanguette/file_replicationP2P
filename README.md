# Système P2P de Réplication de Fichiers

Projet académique — Master IL, UNCHK  
Cours : Systèmes d'Information Distribués  
Encadrant : Dr. Mahamadou TOURE

---

## Description

Système distribué de partage de fichiers en architecture Peer-to-Peer (P2P) avec **réplication automatique configurable** et **tolérance aux pannes**. Chaque nœud est une instance indépendante de l'application Spring Boot qui communique directement avec ses pairs sans serveur central.

**Caractéristiques** :
- Réplication N-way (facteur configurable)
- Recherche distribuée sur les peers
- Gestion des pannes nœuds
- Stockage fichiers local (NIO)

---

## Stack technique

- Java 17
- Spring Boot 3.4.5
- Spring Web (API REST)
- Stockage fichiers local (`java.nio.file`)

---

## Architecture

```
Nœud A (5000)  ←──────────────→  Nœud B (5001)
      ↑                                  ↑
      └──────────────────────────────────┘
                    Nœud C (5002)
```

Chaque nœud possède :
- Son propre dossier de stockage local (`storage_node_XXXX/`)
- Une liste de peers connus
- Une API REST complète

---

## Prérequis

- Java 17+
- Maven (ou utiliser `./mvnw`)

---

## Lancer les nœuds

### Option 1 — Script automatique (3 nœuds d'un coup)

```bash
./launch-nodes.sh
```

### Option 2 — Lancement manuel (un terminal par nœud)

**Terminal 1 — Nœud A (port 5000) :**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=node-a
```

**Terminal 2 — Nœud B (port 5001) :**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=node-b
```

**Terminal 3 — Nœud C (port 5002) :**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=node-c
```

### Arrêter tous les nœuds

```bash
kill $(lsof -ti:5000,5001,5002)
```

---

## API REST

| Méthode | Endpoint | Description |
|---|---|---|
| `POST` | `/files/{filename}` | Upload un fichier (sauvegarde locale + réplication) |
| `GET` | `/files/{filename}` | Download un fichier (local, puis recherche sur les peers) |
| `GET` | `/files` | Liste les fichiers stockés localement |
| `GET` | `/node/info` | Infos du nœud (port, storage, peers, fichiers) |
| `POST` | `/files/replicate/{filename}` | Endpoint interne de réplication (inter-nœuds) |

---

## Scénarios de démonstration

### 1. Upload et réplication automatique

```bash
# Upload sur le nœud A
curl -X POST http://localhost:5000/files/rapport.txt \
  -H "Content-Type: application/octet-stream" \
  --data-binary "Contenu du rapport"

# Vérifier la réplication sur le nœud B
curl http://localhost:5001/files/rapport.txt

# Vérifier les fichiers locaux des deux nœuds
curl http://localhost:5000/node/info
curl http://localhost:5001/node/info
```

### 2. Recherche distribuée

```bash
# Uploader uniquement sur le nœud B
curl -X POST http://localhost:5001/files/photo.jpg \
  -H "Content-Type: application/octet-stream" \
  --data-binary @photo.jpg

# Télécharger depuis le nœud A (recherche automatique sur les peers)
curl http://localhost:5000/files/photo.jpg -o photo_recupere.jpg
```

### 3. Tolérance aux pannes

```bash
# 1. Uploader un fichier (il est répliqué selon le facteur configuré)
curl -X POST http://localhost:5000/files/important.txt \
  -H "Content-Type: application/octet-stream" \
  --data-binary "Données importantes"

# 2. Arrêter le nœud A
kill $(lsof -ti:5000)

# 3. Le fichier reste accessible via les autres nœuds
curl http://localhost:5001/files/important.txt
```

### 4. Facteur de réplication

```bash
# Avec replication-factor: 2, un seul peer reçoit la copie
curl -X POST http://localhost:5000/files/data.bin \
  -H "Content-Type: application/octet-stream" \
  --data-binary @large-file.bin
# → Copie envoyée à 1 peer parmi [5001, 5002]

# Avec replication-factor: 3 (ou aucun peer injoignable)
# → Copie envoyée à tous les peers disponibles
```

---

## Configuration

Chaque profil dispose de son fichier `application-node-X.yml` dans `src/main/resources/` :

```yaml
# Exemple : application-node-a.yml
server:
  port: 5000

node:
  storage: storage_node_5000
  replication-factor: 2        # Nombre de copies (défaut : 2)
  peers:
    - http://localhost:5001
    - http://localhost:5002
```

### Facteur de réplication

| Valeur | Comportement |
|---|---|
| `1` | Pas de réplication (stockage local uniquement) |
| `2` | Réplication sur 1 peer (total : 2 copies) |
| `3` | Réplication sur 2 peers (total : 3 copies) |
| `N` | Réplication sur N-1 peers sélectionnés aléatoirement |

**Exemple** : `replication-factor: 2` avec 3 peers :
- Upload sur Nœud A → copie envoyée à 1 peer parmi les 2 disponibles
- Améliore la performance vs réplication sur tous les peers
- Choisit aléatoirement pour équilibrer la charge

---

## Structure du projet

```
src/main/
├── java/.../
│   ├── config/
│   │   ├── AppConfig.java        # Bean RestTemplate
│   │   └── NodeConfig.java       # Lecture config (storage, peers)
│   ├── controller/
│   │   ├── FileController.java   # Endpoints fichiers
│   │   └── NodeController.java   # Endpoint /node/info
│   └── service/
│       └── FileService.java      # Logique P2P (stockage, réplication, recherche)
└── resources/
    ├── application.yml           # Config par défaut (port 5000)
    ├── application-node-a.yml    # Profil nœud A
    ├── application-node-b.yml    # Profil nœud B
    └── application-node-c.yml    # Profil nœud C
```

---

## Implémentation

✅ **Architecture P2P** — 3 nœuds indépendants, sans serveur central  
✅ **Réplication** — automatique avec facteur configurable  
✅ **Recherche distribuée** — fallback sur les peers  
✅ **Tolérance aux pannes** — gestion des nœuds injoignables  
✅ **Scripts & documentation** — README, workflow.md, launch-nodes.sh  

### Améliorations par rapport au cahier des charges

- **Facteur de réplication** — contrôle du nombre de copies au lieu de "tous les peers"
- **Sélection aléatoire** — équilibre la charge de réplication
- **Stockage fichiers local** — au lieu de MySQL (plus performant, plus P2P-like)
- **Logs structurés** — [LOCAL], [REPLICATION], [SEARCH] pour traçabilité

## Évaluation

| Critère | Pondération | Statut |
|---|---|---|
| Architecture P2P | 30% | ✅ Implémenté |
| Réplication | 25% | ✅ Implémenté + facteur configurable |
| Recherche distribuée | 20% | ✅ Implémenté |
| Tolérance aux pannes | 15% | ✅ Implémenté |
| Qualité du code & démo | 10% | ✅ Code propre, logs, scripts, docs |
