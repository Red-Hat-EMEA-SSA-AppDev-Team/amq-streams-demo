# Python App

## Create virtual environment

```sh
python3 -m venv .env
source .env/bin/activate.fish
pip install -r app-src/requirements.txt 
```

## Local run

```sh
podman run -it -v ./app-src:/opt/app-root/src:Z registry.access.redhat.com/ubi8/python-39:latest python app.py
```

## Local build and run

```sh
podman build -t db-watcher .
podman run -it db-watcher:latest
```

remove:

```sh
podman image rm -f db-watcher:latest
```

## OpenShift deployment

build and launch:

```sh
oc new-build --strategy docker --binary --name=db-watcher
oc start-build db-watcher --from-dir . --follow
oc new-app db-watcher
```

remove all:

```sh
oc delete all --selector="app=db-watcher"
oc delete all --selector="deployment=db-watcher"
oc delete all --selector="build=db-watcher"
```