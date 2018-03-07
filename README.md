[![wtfpl logo](http://www.wtfpl.net/wp-content/uploads/2012/12/wtfpl-badge-4.png)](http://www.wtfpl.net)
# Failsafe Liquibase Locking with Kubernetes Support
Alternate Liquibase Locking solution which makes able an application to recover from a terminated Schema update using Kubernetes API.

## What is Liquibase?
Liquibase is a database version control system for relational databases [http://liquibase.org/](http://liquibase.org/)

## What is Kubernetes?
Kubernetes is a container orchestration platform [http://kubernetes.io/](http://kubernetes.io/)

## What's the purpose of this project?
Liquibase Locks the database while it performs schema updates and when it checks which change sets are executed.
If an application instance stops during this period the database remain locked and it prevents any other instances to lock the database, therefore no other instance can be started until the database is manually fixed.

This behavior is OK for an application which is deployed manually especially if that application runs on a single instance, but it's not acceptable in a multi instance environment where an automated system starts and stops instances in order to match the actual load.
Kubernetes is solution for managing such environments, it can continuously monitor the running services and start new instances when an instance goes down or the load increases.
Unfortunately the locking mechanism mentioned above can compromise the high availability by rendering new instances useless.

## How is it working?
Fortunately Liquibase is easily extendable and Kubernetes has a great API.

This library is only active when a project is running in a [Kubernetes Pod](https://kubernetes.io/docs/concepts/workloads/pods/pod/), in this case the API client can connect without additional authentication and check the status of the Pods, so it can decide that the pod which locked the database is still running or not.
When Liquibase checks for it's lock entry, this library will release the current lock if the pod it's granted to is inactive or is the same as the pod which tries to lock the db (Container was restarted)

Currently two changes are implemented:
### Custom lockedby column value in databasechangeloglock table
In the Standard implementation the lockedby value is intended to provide debug information for human operators.
In this version it's changed to *namespace:podName* format and it's used to decide which pod holds the lock.

### Custom Lock Service
The Standard implementation checks for the lock and if the database is locked it waits for a while and if the lock can not be obtained, it fails.
This version checks the lockedby attribute and looking up the pod to decide that the lock is still relevant or not, and if it's obsolete removes it.
This allows the system to recover from a failure during schema update.

*A partial schema update can cause problems if it's not rolled back properly, I tested this solution with PostgreSQL and according to my experiences Liquibase can rollback changes when it's used with PostgreSQL, but it can leave behind a dirty DB Schema with some other database engines*

## What happens when the application runs outside Kubernetes?
The library checks for connection with the Kubernetes API and if it's not able to connect the custom locking will not be activated, everything works as in the standard implementation.

---
# How to use this library
Using the library is quite easy: it has to be added as a dependency and 2 environment variables has to be set.

## Maven/Gradle etc. dependency
Currently no binaries are publicly available, so please, as of now, build and host it for your self. My goal is to make it available in Maven Central or Sonatype OSS when it is considered stable.

## Kubernetes Deployment config
Two environment variables are required:
- POD_NAME
- POD_NAMESPACE

These can be provided by utilizing [Kubernetes Donward API](https://kubernetes.io/docs/tasks/inject-data-application/downward-api-volume-expose-pod-information/#capabilities-of-the-downward-api)
The required [environment variable configuration](https://kubernetes.io/docs/tasks/inject-data-application/define-environment-variable-container/) looks like this:

```yaml
env:
- name: POD_NAME
  valueFrom:
    fieldRef:
      fieldPath: metadata.name
- name: POD_NAMESPACE
  valueFrom:
    fieldRef:
      fieldPath: metadata.namespace
```
