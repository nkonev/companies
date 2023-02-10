# Play with companies and drafts
```bash
curl -Ss -H 'X-Userid: c092a0ad-8148-4291-9194-6c3d12a1120a' -H 'Content-Type: application/json' -X POST -d '{"name": "Third company", "metadata": {"a": "b", "c": 1}}' --url 'http://localhost:8080/company' | jq '.'

curl -Ss --url 'http://localhost:8080/company' | jq '.'

curl -Ss --url 'http://localhost:8080/company/d776630f-9dd1-41e0-b2b7-b4077a40cb84'

curl -Ss -H 'X-Userid: c092a0ad-8148-4291-9194-6c3d12a1120a' -H 'Content-Type: application/json' -X POST --url 'http://localhost:8080/company/d776630f-9dd1-41e0-b2b7-b4077a40cb84/draft' | jq '.'

curl -Ss --url 'http://localhost:8080/company/d776630f-9dd1-41e0-b2b7-b4077a40cb84/drafts' | jq '.'

curl -Ss -H 'X-Userid: c092a0ad-8148-4291-9194-6c3d12a1120a' -H 'Content-Type: application/json' -X PUT -d '{"message": "Patch note", "company": { "name": "Third company patched 100505"}}' --url 'http://localhost:8080/company/d776630f-9dd1-41e0-b2b7-b4077a40cb84/draft/88b03065-71cd-4a6d-b717-78f779402a8b' | jq '.'

curl -Ss -H 'X-Userid: c092a0ad-8148-4291-9194-6c3d12a1120a' -H 'Content-Type: application/json' -X PUT -d '{"message": "Approve note"}' --url 'http://localhost:8080/company/d776630f-9dd1-41e0-b2b7-b4077a40cb84/draft/88b03065-71cd-4a6d-b717-78f779402a8b/approve' | jq '.'

```

# Load testing

## 50000 branches
```
docker-compose up -d
./mvnw -DenableLoadTests=true -Dtest="name.nkonev.examples.companies.VolumeTest#fifty_thousand_branches" test
```


## Create 7000 companies, then add a commit to every company three times
### Run web app in the first terminal window
```
./mvnw spring-boot:run
```

### Then create 7000 companies th another terminal
```
./mvnw -DenableLoadTests=true -Dtest="name.nkonev.examples.companies.VolumeTest#seven_thousand_companies" test
```

### Then create branch for every company three times (sequentially)
```
./mvnw -DenableLoadTests=true -Dtest="name.nkonev.examples.companies.VolumeTest#create_draft_for_each_company" test
./mvnw -DenableLoadTests=true -Dtest="name.nkonev.examples.companies.VolumeTest#create_draft_for_each_company" test
./mvnw -DenableLoadTests=true -Dtest="name.nkonev.examples.companies.VolumeTest#create_draft_for_each_company" test
```

### Then run every-second requests in three terminal windows in parallel
```
watch -n 1 curl -Ss --url 'http://localhost:8080/company'
```

```
watch -n 1 curl -Ss --url 'http://localhost:8080/company'
```

```
watch -n 1 curl -Ss --url 'http://localhost:8080/company'
```

Those requests turned into SQL like
```sql
CALL DOLT_CHECKOUT ('main');
SELECT `company`.`name` AS `name`, `company`.`modified_at` AS `modified_at`, `company`.`id` AS `id`, `company`.`bank_account` AS `bank_account`, `company`.`estimated_size` AS `estimated_size` FROM `company` ORDER BY `company`.`name` ASC LIMIT 0, 20
```

(You can uncomment JDBC logging in `application.yaml`)

Now we can see 20% CPU of dolt in `top`.

I tried to create indexes
```sql
ALTER TABLE company
    ADD INDEX company_name USING HASH (name);

ALTER TABLE company
    ADD INDEX company_name USING BTREE (name);
```
but it doesn't change the CPU load.


### If we start creating branches again CPU of dolt will be 120%
```
./mvnw -DenableLoadTests=true -Dtest="name.nkonev.examples.companies.VolumeTest#create_draft_for_each_company" test
```