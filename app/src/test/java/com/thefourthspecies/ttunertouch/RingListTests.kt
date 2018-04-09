package com.thefourthspecies.ttunertouch

import org.junit.Test
import org.junit.Assert.*

/**
 * Created by Graham on 2018-02-02.
 */
class RingListTests {

    @Test
    fun orderingTest() {
        val rl = RingList<Int>(listOf(4,2,6,1,7,9,2,3,5,8,0))

        val rlList = rl.toList()
        assertEquals(listOf(0,1,2,3,4,5,6,7,8,9), rlList)

        val upFrom5 = rl.iterateFrom(5, Direction.ASCENDING).toList()
        assertEquals(listOf(5,6,7,8,9,0,1,2,3,4), upFrom5)

        val downFrom7 = rl.iterateFrom(7, Direction.DESCENDING).toList()
        assertEquals(listOf(7,6,5,4,3,2,1,0,9,8), downFrom7)

        val up9to5 = rl.iterateUntilExcluding(9, 5, Direction.ASCENDING).toList()
        assertEquals(listOf(9,0,1,2,3,4), up9to5)

        val up9to9 = rl.iterateUntilExcluding(9, 9, Direction.ASCENDING).toList()
        assertEquals(listOf<Int>(), up9to9)
    }

}