#!/bin/bash

JAR="target/projetPtoP-0.0.1-SNAPSHOT.jar"

echo "=== Build du projet ==="
./mvnw package -DskipTests -q

if [ $? -ne 0 ]; then
  echo "Build échoué. Arrêt."
  exit 1
fi

echo ""
echo "=== Lancement des nœuds ==="

java -jar $JAR --spring.profiles.active=node-a > logs/node-a.log 2>&1 &
echo "Nœud A démarré sur le port 5000 (PID $!)"

java -jar $JAR --spring.profiles.active=node-b > logs/node-b.log 2>&1 &
echo "Nœud B démarré sur le port 5001 (PID $!)"

java -jar $JAR --spring.profiles.active=node-c > logs/node-c.log 2>&1 &
echo "Nœud C démarré sur le port 5002 (PID $!)"

echo ""
echo "Attente du démarrage des nœuds..."
sleep 8

echo ""
echo "=== Vérification ==="
curl -s http://localhost:5000/node/info | python3 -m json.tool 2>/dev/null && echo "Nœud A OK" || echo "Nœud A non joignable"
curl -s http://localhost:5001/node/info | python3 -m json.tool 2>/dev/null && echo "Nœud B OK" || echo "Nœud B non joignable"
curl -s http://localhost:5002/node/info | python3 -m json.tool 2>/dev/null && echo "Nœud C OK" || echo "Nœud C non joignable"

echo ""
echo "Logs disponibles dans logs/node-a.log, logs/node-b.log, logs/node-c.log"
echo "Pour arrêter tous les nœuds : kill \$(lsof -ti:5000,5001,5002)"
