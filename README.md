# Integrated-EEA-and-Driving-Simulator-Coupling

![Alt text](ScreenshotOpends.jpg?raw=true "Title")
#Project Summary:

This project provides a co-simulation framework for realistic driving simulation, dedicated to ADAS/HAD validation. 

Specifically, this project contains the coupling of the two open-source simulators: 
driving simulator OpenDS from DFKI (Deutsches Forschungszentrum für Künstliche Intelligenz, https://opends.dfki.de/) and Ptolemy II from UC Berkeley (https://ptolemy.berkeley.edu/ptolemyII/index.htm).

OpenDS provides a powerful solution to realistic vehicle dynamics, traffic flow, traffic light and physics (weather, light, frictions) simulation and is used in this project to provide early prototyping of ADAS/HAD modules of autonomous cars.
Ptolemy II features an actor-oriented, multi-domain simulation with abundant built-in pre-programmed functional blocks in type conversion, database, Machine Learning, Co-Simulation, etc and can be utilized as automotive E/E Architecture simulator.
This work combines both simulators so an autonumous driving car as well as its physics interaction with the environment can be simulated in OpenDS, whereas the functionalities of electrical and electronic systems are defined in Ptolemy II.

#Getting Started


These instructions will help you through the deployment of the proposed simulation framework.

#Prerequisites:

1. Install the driving simulator at this website https://opends.dfki.de/packages/. (Version: OpenDS Free or above)
2. Install latest version of Ptolemy II from https://ptolemy.berkeley.edu/ptolemyII/index.htm. 
3. Install latest jdk from Oracle: https://www.oracle.com/java/technologies/javase-downloads.html
4. (Recommended) Install an integrated development environment (IDE) such as Eclipse. 

#Installing this package:
1. Just copy the files in this repository to the respective projects and overwrite any file that already exists.

