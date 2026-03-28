USE farmstay_db;

INSERT INTO user_account(username, password, salt, display_name, user_type, status, balance, created_at, updated_at)
SELECT 'admin',
       '3f3f7946f6b2a64ace7034337d42e5f923fe29d83e9ae07b13a8951b006001fb',
       'adm1n6',
       '平台管理员',
       'admin',
       'ACTIVE',
       0,
       NOW(),
       NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM user_account WHERE username = 'admin' AND user_type = 'admin'
);
