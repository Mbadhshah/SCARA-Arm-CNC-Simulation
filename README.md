# SCARA Arm CNC Printer Simulation

This project is a Java-based simulation of a SCARA (Selective Compliance Assembly Robot Arm) robotic arm designed for CNC printing. It uses inverse kinematics to control a 2-link arm and includes a Swing-based GUI for real-time visualization, manual coordinate input, and G-code file processing.

## Features
- **Real-time Visualization**: Displays the SCARA arm's movement on a 230x230 mm virtual bed.
- **Inverse Kinematics**: Calculates joint angles (theta1, theta2) for precise positioning.
- **Coordinate Input**: Move the arm manually via `X<value>Y<value>` commands.
- **G-code Support**: Upload and process G-code files (e.g., `G0`, `G1`, `M3`, `M5`) for automated tracing.
- **Tracing Mode**: Draws magenta paths when tracing is enabled.

## Demo

![{9C516505-0041-4753-AC0C-BB642E9FCB7C} (1)](https://github.com/user-attachments/assets/56fca439-1272-4743-8f41-a8489cfe8d6d)


## Prerequisites
- Java Development Kit (JDK) 8 or higher
- A Java IDE (e.g., IntelliJ IDEA, Eclipse) or command-line tools

