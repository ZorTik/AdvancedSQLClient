<div align="center">
<img src="https://user-images.githubusercontent.com/67344817/230640574-508510ea-7c59-4606-af34-c1ab7981a767.png" width="130px">
<!-- <img src="https://user-images.githubusercontent.com/67344817/201498814-84314078-3f88-4930-aec1-d3f437e5c9e8.png" width="130px"> -->


# AdvancedSQLClient
Ultimate SQL client with intuitive query builders, Json support and more!<br>
Head to new <a href="https://github.com/ZorTik/AdvancedSQLClient/wiki">wiki</a> for usage tutorial & quickstart!

![Badge](https://img.shields.io/jitpack/version/com.github.ZorTik/AdvancedSQLClient?style=for-the-badge) ![Badge](https://img.shields.io/github/license/ZorTik/AdvancedSQLClient?style=for-the-badge)
</div>



<!--<p align="center">
<img src="https://user-images.githubusercontent.com/67344817/183105393-af39026f-b059-4096-a880-1fe0e93eeeee.png" width="100%"></img>
</p>-->

## Installation
You can add AdvancedSQLClient to your build path using Maven or Gradle. You can also shade&relocate it using shade plugin to have it's unique build path.

<a href="https://github.com/ZorTik/AdvancedSQLClient/wiki">Installation & Usage on Wiki</a>

## Examples
```java
@Table("users")
public interface UserRepository {
  @Save
  QueryResult save(User user);
}

UserRepository repository = connection.createProxy(UserRepository.class);
repository.save(new User("User"));

// TIP: We support query builders too! Check wiki section.
```
```java
connection.insert()
        .into("users", "firstname", "lastname")
        .values("John", "Doe")
        .execute();
```

## Code of Conduct
This repository contains some basic rules specified in Code of Conduct file.<br>

<a href="https://github.com/ZorTik/AdvancedSQLClient/blob/master/CODE_OF_CONDUCT.md">Code of Conduct</a><br>
<a href="https://www.flaticon.com/free-icons/database" title="database icons">Database icons created by Freepik - Flaticon</a>
