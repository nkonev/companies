-- insert here the branch permissions to reduce new branch creation degradation https://github.com/dolthub/dolt/issues/5175#issuecomment-1402109985
DELETE FROM dolt_branch_control;
INSERT INTO dolt_branch_control VALUES ('%', '%', 'root', '%', 'admin');
