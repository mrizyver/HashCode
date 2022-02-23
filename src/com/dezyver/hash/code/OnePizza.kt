package com.dezyver.hash.code

import java.io.File
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.system.measureTimeMillis

const val basic = "input/b_basic.in.txt"
const val coarse = "input/c_coarse.in.txt"
const val difficult = "input/d_difficult.in.txt"
const val elaborate = "input/e_elaborate.in.txt"

data class Wish(val likes: Set<String>, val dislikes: Set<String>)

class Input(val list: List<Wish>, val ingridients: Set<String>) : Iterable<Wish>{
    override fun iterator(): Iterator<Wish> {
        return list.iterator()
    }
}

typealias Solution = Set<String>

class EstimatedSolution(val score: Int, val solution: Solution)

fun main() {
    val inputList = parseInput(File(elaborate))
    val ingredients = HashSet<String>()
    for (wish in inputList) {
        ingredients.addAll(wish.likes)
        ingredients.addAll(wish.dislikes)
    }
    val input = Input(inputList, ingredients)
    val availableProcessors = Runtime.getRuntime().availableProcessors()
    val generations = ArrayBlockingQueue<List<Solution>>(availableProcessors * 2, false)
    generations.offer(listOf(bogdan(inputList)))
    val estimates = ArrayBlockingQueue<List<EstimatedSolution>>(availableProcessors * 2, false)
    var gen = 0
    var totalBest: EstimatedSolution? = null
    repeat(availableProcessors - 1){
        thread {
            while (true) {
                // 1 estimate current generation
                val generation: List<Solution> = generations.take()
                estimates.offer(generation.map { s -> EstimatedSolution(calculateScore(input.list, s), s) })
            }
        }
    }
    while (true) {
        val estimated = estimates.take()
        // 2 find the best solution, log generation number, max score of this generation
        val best: EstimatedSolution = estimated.maxByOrNull { it.score } ?: continue
        if (totalBest?.score ?: 0 < best.score){
            totalBest = best
        }
        println("Generation ${gen++}, max score ${best.score}, ingrs ${best.solution.size}, the best ${totalBest?.score}, generations ${generations.size}, estimates ${estimates.size}")
        // 3 run selection
        val selected = selection(estimated)
        // 4 run mutation
        val times = if (generations.size < availableProcessors) 2 else 1
        repeat(times) {
            generations.offer(mutate(input, selected))
        }
    }
}

fun makeInitialGeneration (input: Input): List<Solution> {
    val times = 100
    val solution = ArrayList<Set<String>>(times)
    repeat(times) {
        val element = input.fold(HashSet()) { result: Set<String>, wish: Wish ->
            if (Random.nextInt() % 50 != 0) result
            else result + wish.likes + wish.dislikes
        }
        solution.add(element)
    }
    return solution
}

fun selection (solutions: List<EstimatedSolution>): List<EstimatedSolution> {
    val bestScore = solutions.maxOf { it.score }
    val worstScore = solutions.minOf { it.score }
    if (solutions.size < 100) return solutions
    return solutions.filter {
        val probability = (it.score.toFloat() - worstScore) / (bestScore.toFloat() - worstScore) + 0.1f
        Random.nextFloat() < probability
    }
}

fun mutate (input: Input, solutions: List<EstimatedSolution>): List<Solution> {
    val mutations = LinkedList<Solution>()
    for (solution: EstimatedSolution in solutions) {
        if (solutions.size < 100) {
            val temp = solution.solution.toMutableSet()
            if (Random.nextBoolean() && temp.size > 1){
                temp.remove(temp.random())
            } else {
                temp.add(input.ingridients.random())
            }
            mutations.add(temp)
        }

        val client = input.list.random()
        val temp: Solution = solution.solution + client.likes - client.dislikes
        mutations.add(temp)
    }
    return mutations
}

fun groupWishes(wishes: List<Wish>): List<Wish> {
    val sortedWishes = wishes.sortedWith { o1, o2 -> (o1.dislikes.size - o1.likes.size) - (o2.dislikes.size - o2.likes.size) }
    val groupedWishes = ArrayList<Wish>()
    client@ for (wish in sortedWishes) {
        group@ for ((index, group) in groupedWishes.withIndex()) {
            val likesConflict: Boolean = wish.likes.fold(false) { acc, str -> group.dislikes.contains(str) || acc }
            val dislikesConflict: Boolean = wish.dislikes.fold(false) { acc, str -> group.likes.contains(str) || acc }
            if (!likesConflict && !dislikesConflict){
                groupedWishes[index] = Wish(group.likes + wish.likes, group.dislikes + wish.dislikes)
                continue@client
            }
        }
        groupedWishes.add(wish)
    }
    return groupedWishes
}

fun parseInput(file: File): List<Wish> {
    val lines = file.readLines()
    val num = lines.first().trim().toInt()
    var index = 0
    val output = ArrayList<Wish>(num)
    while (num * 2 > index) {
        val liked = lines[++index].split(" ")
        val disliked = lines[++index].split(" ")
        output.add(Wish(liked.subList(1, liked.size).toSet(), disliked.subList(1, disliked.size).toSet()))
    }
    return output
}

fun calculateScore (wishes: Collection<Wish>, recipe: Set<String>): Int {
    return wishes.fold(0) { score, wish ->
        val wishFits = recipe.containsAll(wish.likes) && !wish.dislikes.fold(false) { b, dis -> recipe.contains(dis) || b }
        if (wishFits) score + 1 else score
    }
}

fun bogdan(wishes: List<Wish>): Solution {
    var maxScore = 0
    var bestRecipe =  HashSet<String>()

    fun checkBest (recipe: HashSet<String>) {
        val score = calculateScore(wishes, recipe)
        if (score > maxScore) {
            maxScore = score
            bestRecipe = recipe
        }
    }

    for (wish in wishes) {
        val needToAdd = wish.likes.filter { like -> !bestRecipe.contains(like) }
        val needToRemove = wish.dislikes.filter{like -> bestRecipe.contains(like) }
        val recipe = HashSet<String>(bestRecipe)
        needToAdd.forEach{item -> recipe.add(item)}
        needToRemove.forEach{item -> recipe.remove(item)}
        checkBest(recipe)
    }
    return bestRecipe
}