create table email (
  id int primary key,
  address varchar(100),
  users_id int
);
create table users (
  id int primary key,
  first_name varchar(40),
  last_name varchar(40)
);
