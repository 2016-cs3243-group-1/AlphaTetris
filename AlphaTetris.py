#!/usr/bin/python

import subprocess
import sys
import random
import signal
import operator
from math import floor
from multiprocessing import Pool

numAttempts = 1

# have to keep the rungame process in a method outside the class due to how multiprocessing pool works
def runGame(weights):
    signal.signal(signal.SIGINT, signal.SIG_IGN)

    command = ["java", "PlayerSkeleton"] + [str(x) for x in weights]
    totalRowsCleared = 0
    totalTurns = 0
    for attempt in range(numAttempts):
        process = subprocess.Popen(command, stdout=subprocess.PIPE)
        numRowsCleared, numTurns = process.stdout.read().decode("utf-8").strip("\r\n").split(" ")
        totalRowsCleared += int(numRowsCleared)
        totalTurns += int(numTurns)
    return (totalRowsCleared // numAttempts, totalTurns // numAttempts)

class AlphaTetris():
    """Genetic Algorithm to optimise weights for heuristics in PlayerSkeleton.java"""

    pool_size = 4
    defaultWeights = [
        -0.510066, # Aggregate column heights
        -0.184483, # Bumpiness
        0, # Max height
        -0.6, # Num of holes created
        0.760666 # Num of completed rows
    ]

    # True if weight should be positive
    shouldWeightBePositive = [
        False,
        False,
        False,
        False,
        True
    ]

    def __init__(self):
        self.pool = Pool(AlphaTetris.pool_size)
        self.populationCount = 64    # Must be even
        self.chooseParentsRate = 0.1
        self.regenParentsRate = 0.05
        self.mutationRate = 0.05    # [0, 1]
        self.generation = 1
        self.start([AlphaTetris.defaultWeights] + [self.generateWeights() for i in range(self.populationCount - 1)])

    def start(self, populationWeights):
        print("Generation " + str(self.generation))

        choices = []
        pool = Pool(self.pool_size)

        try:
            scores = pool.map(runGame, populationWeights)
        except KeyboardInterrupt as ki:
            pool.join()
            pool.terminate()
        else:
            totalRowsCleared = 0
            for (numRowsCleared, numTurns), weights in zip(scores, populationWeights):
                print("Rows cleared: " + str(numRowsCleared) + "\tNumber of turns: " + str(numTurns) + "\tWeights used: " + str(weights))
                totalRowsCleared += numRowsCleared
                choices.append((weights, numRowsCleared + numTurns))

            parents = self.selectParents(choices)
            self.crossover(parents)
            self.mutate(parents)

            self.generation += 1
            print("Average number of rows cleared: " + str(totalRowsCleared // self.populationCount))
            self.start(parents)

    #### Initial population
    def generateWeights(self, delta=0.01):
        return [self.generateWeight(idx, weight, delta) for idx, weight in enumerate(AlphaTetris.defaultWeights)]

    def generateWeight(self, baseWeightIndex, baseWeight, delta=0.01):
        # isPositive = AlphaTetris.shouldWeightBePositive[baseWeightIndex]

        # weight = random.gauss(baseWeight, min(0.3, (1 - abs(baseWeight))/2)) # TODO change standard deviation
        # if isPositive:   # Weight should be positive
        #     return abs(weight)
        # else:
        #     return -abs(weight)
        return baseWeight + random.uniform(-delta, delta)


    #### Selection
    def selectParents(self, choices):
        choices.sort(key=lambda choice: choice[1], reverse=True)
        print([choice[1] for choice in choices])
        parents = [self.selectParent(choices[:int(self.chooseParentsRate * self.populationCount)]) for i in range(self.populationCount)]
        numParentsToRegen = int(self.regenParentsRate * self.populationCount)
        return [parent[0] for parent in parents[:-numParentsToRegen]] + [self.generateWeights(0.1) for i in range(numParentsToRegen)]

    def selectParent(self, choices):
        total = sum(score for weights, score in choices)
        randomVar = random.uniform(0, total)
        currentSum = 0

        for weights, score in choices:
            if currentSum + score >= randomVar:
                return (weights, score)
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
                mutation_target = random.randint(0, len(AlphaTetris.defaultWeights) - 1)
                parent[mutation_target] = self.generateWeight(mutation_target, parent[mutation_target])



if __name__ == '__main__':
	AlphaTetris()