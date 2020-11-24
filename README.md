# Architecture

The application is composed of different Microservices:

- Subreddit Service, which allows users to browse, create & manage subreddits;
- Post Service, which allows users to browse, create & manage posts;
- Comment Service, which allows users to browse, create & manage comments;
- User Service, which manages user profiles.

The technology stack:
- Each microservice is a Spring Boot app;
- Reactive MongoDB is used to store the documents;
- The communication protocol used is RSocket;
- Security is managed through an OAuth2 OIDC provider, in this case I went for Keycloak.
