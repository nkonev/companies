# Play with companies and drafts
```bash
curl -Ss -H 'X-Userid: c092a0ad-8148-4291-9194-6c3d12a1120a' -H 'Content-Type: application/json' -X POST -d '{"name": "Third company"}' --url 'http://localhost:8080/company' | jq '.'

curl -Ss --url 'http://localhost:8080/company' | jq '.'

curl -Ss -H 'X-Userid: c092a0ad-8148-4291-9194-6c3d12a1120a' -H 'Content-Type: application/json' -X POST --url 'http://localhost:8080/company/d776630f-9dd1-41e0-b2b7-b4077a40cb84/draft' | jq '.'

curl -Ss --url 'http://localhost:8080/company/d776630f-9dd1-41e0-b2b7-b4077a40cb84/drafts' | jq '.'

curl -Ss -H 'X-Userid: c092a0ad-8148-4291-9194-6c3d12a1120a' -H 'Content-Type: application/json' -X PUT -d '{"message": "Patch note", "company": { "name": "Third company patched 100505"}}' --url 'http://localhost:8080/company/d776630f-9dd1-41e0-b2b7-b4077a40cb84/draft/88b03065-71cd-4a6d-b717-78f779402a8b' | jq '.'

curl -Ss -H 'X-Userid: c092a0ad-8148-4291-9194-6c3d12a1120a' -H 'Content-Type: application/json' -X PUT -d '{"message": "Approve note"}' --url 'http://localhost:8080/company/d776630f-9dd1-41e0-b2b7-b4077a40cb84/draft/88b03065-71cd-4a6d-b717-78f779402a8b/approve' | jq '.'

```

# Repro
```
docker-compose up -d
./mvnw -DenableLoadTests=true -Dtest="name.nkonev.examples.companies.VolumeTest#thirty_five_thousands_branches" test
```