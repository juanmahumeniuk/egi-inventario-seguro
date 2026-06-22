#!/usr/bin/env bash
# Despliega (o actualiza) el ecosistema en Kubernetes.
# Uso: deploy-k8s.sh <imagen-completa>
# Ejemplo: deploy-k8s.sh ghcr.io/juanmahumeniuk/inventario-web:abc1234

set -euo pipefail

IMAGE="${1:?Usage: deploy-k8s.sh <image>}"
NAMESPACE="inventario-seguro"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT"

echo "==> Aplicando manifiestos desde ${ROOT}/k8s/"
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/storage/mongodb-pvc.yaml
kubectl apply -f k8s/deployments/mongodb.yaml
kubectl apply -f k8s/services/mongodb.yaml
kubectl apply -f k8s/deployments/inventario-web.yaml
kubectl apply -f k8s/services/inventario-web.yaml
kubectl apply -f k8s/ingress/inventario-ingress.yaml
kubectl apply -f k8s/ingress/ingress-nginx-nodeport.yaml
kubectl apply -f k8s/network-policies/

echo "==> Actualizando imagen de inventario-web a ${IMAGE}"
kubectl set image "deployment/inventario-web" "inventario-web=${IMAGE}" -n "${NAMESPACE}"

echo "==> Esperando rollout..."
kubectl rollout status "deployment/inventario-web" -n "${NAMESPACE}" --timeout=300s

echo "==> Estado de pods:"
kubectl get pods -n "${NAMESPACE}"
