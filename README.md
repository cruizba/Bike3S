[![Build Status](https://travis-ci.org/stimonm/Bike3S.svg?branch=master)](https://travis-ci.org/stimonm/Bike3S)
# Bike3S-Simulator - Introduction

Bike3S is a simulator created for the purpose of testing different behaviors in real bike sharing systems. Bike sharing systems allow citizens to move between different places in an simple and economical way. Bike3S offers us the possibility to execute situations using different infrastructure configurations, user models and balancing algorithms.

The motivation of this project is to search strategies to balance the system resources using systems, typically, based on incentives, which recommend the users to rent or return a bike in a certain station to contribute to the system balancing

Here you'll find all the necessary documentation to use and develop new features in Bike3S.

## For Users 

If you just want to run the simulator with preconfigured user behaviors in different cities around the world, just follow the next guide:
[User Guide](#user-guide)

## For Developers

If you are a developer or researcher and wants to create new things for the simulator, you should start here. With this guide you will learn to: 

- Configure and prepare your system for development.
- Configure you're favorite IDE.
- Learn the architecture and fundamentals of the simulator to implement new things.
- Implement different users behaviors.
- Implement your own recommendation system.

And more... Feel free to see how to make all this fun stuff reading the developers Guide:

[Developer Guide](#developers-guide)
## F.A.Q.
[F.A.Q. link](#faq-link)

# <a id="user-guide"></a>Users Guide 
TO DO
# <a id="developers-guide"></a>Developers Guide 

## Prerequisites

1. JDK 1.8
2. Maven >= 3.5
3. Node.js >= 8.9
4. Git

Please make sure that all the binaries are registered in your PATH.

The package manager NPM is also required but is usually bundled with the Node.js installer.

## Getting Started for Development

This project is development environment agnostic. You can use a IDE or just the command line

### General overview of the spftware architecture 

The project is separated in two mains parts **backend** and a **frontend**.

![Arquitecture´s image of the system](/assets/Arquitecture_10.png)

The **backend** is related to all the simulation logic and is implemented in Java. The folder `/backend-bikesurbanfleets` contains this part of the project.

The **frontend** is related to all the GUI and data analysis of the simulations. It is implemented in *TypeScript*, using *Angular* and *Electron*. The folder `/frontend-bikesurbanfleets` contains this part of the project.

### Setup

1. Firstly of all, check that all prerequisites are installed.
2. Execute this commands in the project directory.

```
    git clone https://github.com/gia-urjc/Bike3S-Simulator.git
    cd Bike3S-Simulator
    npm install
    node fuse configure:dev
```


If no errors appeared you have now all prepared for all you want. Run the program, compile simulator, etc... Next sections are just commands to compile and run the system.

### Execute Frontend

You can now execute your program and try it just by running:
```
    node fuse build:frontend
```

### Execute Backend

TODO - Execute Backend users generator and Backend Users Core example

### Build From Command Line 

To build the backend, execute this command:
```
    node fuse build:dev-backend
```
To build the frontend and execute the GUI:
```
    node fuse build:frontend
```
To build all project:
```
    node fuse build:dist
```

## Distrubute 

To distribute and executable or installer for your OS, just run 
`npm run distribute`

Executables are generated in `build/dist/`

## Fundamentals

TODO - Link to pdf developer manual


### Simulate from the command line

Are you too lazy to configure your IDE? No problem, you can execute this commands to execute the simulator.

**Generate users**:

```
node fuse gen-users:dev
```

**Simulate**:

```
node fuse simulate:dev
```

The configuration files of the simulation run via `node fuse gen-users:dev` and `node fuse simulate:dev`, are in the project directory in the folder `/backend-configuration-files`. To test quickly simulations without the GUI, you can edit these configuration files and run these commands. The history will be stored in `/build/history`. 

# <a id="user-guide"></a>F.A.Q 

> What IDEs or Text Editors should I use

First of all, this project is divided in two code bases, a backend (JAVA) and a frontend (TypeScript + Angular + Electron).

This project is prepared for any IDE or Text editor you want. Some editors/IDEs will work for both code bases, some only for one code base. Here you have some basic configuration for the most common IDE's and text editors and the part of the project you can work with it:

[IntelliJ IDEA CE (Backend)](documentation/setup_intellij.md)

[WebStorm (Frontend)](documentation/setup_webstorm.md)

[Visual studio Code (Backend and Frontend)](documentation/setup_vscode.md) RECOMMENDED. The project is configured automatically.

[Eclipse (Backend)](documentation/setup_eclipse.md)

[Netbeans (Backend)](documentation/setup_netbeans.md)

