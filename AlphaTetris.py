#!/usr/bin/python
import subprocess
import sys

class AlphaTetris():
    """Genetic Algorithm to optimise weights for heuristics in PlayerSkeleton.java"""

    defaultWeights = [
        -0.510066, # Aggregate column heights
        -0.184483, # Bumpiness
        0, # Max height
        -0.6, # Num of holes created
        0.760666 # Num of completed rows
    ]

    def __init__(self):
        self.populationCount = 8
        self.weights = AlphaTetris.defaultWeights
        self.start()

    def start(self):
        print("Weights used:", self.weights)
        numRowsCleared, numTurns = self.runGame(self.weights)
        print("Rows cleared:", numRowsCleared, "Number of turns:", numTurns)

    def runGame(self, weights):
        command = ["java PlayerSkeleton"] + [str(x) for x in weights]
        process = subprocess.run(command, shell=True, stdout=subprocess.PIPE)
        return process.stdout.decode("utf-8").split(" ")


if __name__ == '__main__':
	AlphaTetris()