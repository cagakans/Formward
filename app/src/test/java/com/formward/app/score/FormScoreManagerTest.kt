package com.formward.app.score

import org.junit.Assert.assertEquals
import org.junit.Test

class FormScoreManagerTest {

    @Test
    fun perfectNutritionReachesMaximumScore() {
        val score = calculateNutritionScore(
            calorieTarget = 2000.0,
            calorieConsumed = 2000.0,
            proteinTarget = 150.0,
            proteinConsumed = 150.0,
            waterTarget = 2.5,
            waterConsumed = 2.5,
            hasCarbs = true,
            hasFat = true
        )

        assertEquals(40, score)
    }

    @Test
    fun partialNutritionProducesPartialScore() {
        val score = calculateNutritionScore(
            calorieTarget = 2000.0,
            calorieConsumed = 1000.0,
            proteinTarget = 100.0,
            proteinConsumed = 50.0,
            waterTarget = 2.0,
            waterConsumed = 1.0,
            hasCarbs = false,
            hasFat = false
        )

        assertEquals(17, score)
    }

    @Test
    fun emptyNutritionDataProducesZeroScore() {
        val score = calculateNutritionScore(
            calorieTarget = 0.0,
            calorieConsumed = 0.0,
            proteinTarget = 0.0,
            proteinConsumed = 0.0,
            waterTarget = 0.0,
            waterConsumed = 0.0,
            hasCarbs = false,
            hasFat = false
        )

        assertEquals(0, score)
    }

    @Test
    fun missionScoreScalesFromZeroToTwenty() {
        assertEquals(0, calculateMissionScore(0))
        assertEquals(10, calculateMissionScore(50))
        assertEquals(20, calculateMissionScore(100))
    }

    @Test
    fun dailyScoreCombinesAllScoreComponents() {
        val score = calculateDailyScore(
            workoutScore = 40,
            nutritionScore = 40,
            missionScore = 20
        )

        assertEquals(100, score)
    }

    @Test
    fun dailyScoreCannotExceedOneHundred() {
        val score = calculateDailyScore(
            workoutScore = 50,
            nutritionScore = 50,
            missionScore = 30
        )

        assertEquals(100, score)
    }
}