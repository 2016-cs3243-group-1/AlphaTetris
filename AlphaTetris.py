#!/usr/bin/python

import subprocess
import sys
import random

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
        self.populationCount = 4    # Must be even
        self.mutationRate = 0.05    # [0, 1]
        self.generation = 1
        self.start([AlphaTetris.defaultWeights] + [self.generateWeights() for i in range(self.populationCount - 1)])

    def start(self, populationWeights):
        print("Generation", self.generation)

        choices = []
        for weights in populationWeights:
            print("Weights used:", weights)
            numRowsCleared, numTurns = self.runGame(weights)
            choices.append((weights, int(numRowsCleared)))
            print("Rows cleared:", numRowsCleared, "Number of turns:", numTurns)

        parents = self.selectParents(choices)
        self.crossover(parents)
        self.mutate(parents)

        self.generation += 1
        self.start(parents)

    def runGame(self, weights):
        command = ["java", "PlayerSkeleton"] + [str(x) for x in weights]
        process = subprocess.run(command, shell=True, stdout=subprocess.PIPE)
        return process.stdout.decode("utf-8").split(" ")


    #### Initial population
    def generateWeights(self):
        return [self.generateWeight() for i in range(len(AlphaTetris.defaultWeights))]

    def generateWeight(self):
        return random.uniform(-1, 1)


    #### Selection
    def selectParents(self, choices):
        return [self.selectParent(choices) for i in range(self.populationCount)]

    def selectParent(self, choices):
        total = sum(score for weights, score in choices)
        randomVar = random.uniform(0, total)
        currentSum = 0

        for weights, score in choices:
            if currentSum + score >= randomVar:
                return weights
            currentSum += score

        assert False, "Shouldn't get here"


    #### Crossover
    def crossover(self, parents):
        for i in range(0, self.populationCount, 2):
            crosspoint = random.randint(0, len(AlphaTetris.defaultWeights) - 1)
            temp = parents[i][:crosspoint]
            parents[i][:crosspoint] = parents[i+1][:crosspoint]
            parents[i+1][:crosspoint] = temp


    #### Mutation
    def mutate(self, parents):
        for parent in parents:
            if random.uniform(0, 1) < self.mutationRate:
                parent[random.randint(0, len(AlphaTetris.defaultWeights) - 1)] = self.generateWeight()



if __name__ == '__main__':
	AlphaTetris()