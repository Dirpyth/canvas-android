/*
 * Copyright (C) 2019 - present Instructure, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.instructure.student.ui.interaction

import androidx.test.espresso.Espresso
import com.instructure.canvas.espresso.mockCanvas.MockCanvas
import com.instructure.canvas.espresso.mockCanvas.addAccountNotification
import com.instructure.canvas.espresso.mockCanvas.init
import com.instructure.panda_annotations.FeatureCategory
import com.instructure.panda_annotations.Priority
import com.instructure.panda_annotations.TestCategory
import com.instructure.panda_annotations.TestMetaData
import com.instructure.student.ui.utils.StudentTest
import com.instructure.student.ui.utils.tokenLogin
import junit.framework.TestCase.assertNotNull
import org.junit.Test


class DashboardInteractionTest : StudentTest() {
    override fun displaysPageObjects() = Unit // Not used for interaction tests

    @Test
    @TestMetaData(Priority.P0, FeatureCategory.DASHBOARD, TestCategory.INTERACTION)
    fun testNavigateToDashboard() {
        // User should be able to tap and navigate to dashboard page
        val data = getToDashboard(courseCount = 1, favoriteCourseCount = 1)
        dashboardPage.clickInboxTab()
        inboxPage.goToDashboard()
        dashboardPage.assertDisplaysCourse(data.courses.values.first()) // disambiguates via isDisplayed()

        // These get confused by the existence of multiple DashboardPages in the layout
        //dashboardPage.waitForRender()
        //dashboardPage.assertPageObjects()
    }

    @Test
    @TestMetaData(Priority.P1, FeatureCategory.DASHBOARD, TestCategory.INTERACTION)
    fun testDashboardCourses_emptyState() {
        // Empty state should be displayed with a 'Add Courses' button, when nothing is favorited (and courses are completed/concluded)
        // With the new DashboardCard api being used, if nothing is a favorite it will default to active enrollments
        getToDashboard(courseCount = 0, pastCourseCount = 1)
        dashboardPage.assertDisplaysAddCourseMessage()
    }

    @Test
    @TestMetaData(Priority.P1, FeatureCategory.DASHBOARD, TestCategory.INTERACTION)
    fun testDashboardCourses_addFavorite() {
        // Starring should add course to favorite list

        val data = getToDashboard(courseCount = 2,favoriteCourseCount = 1)
        val nonFavorite = data.courses.values.filter {x -> !x.isFavorite}.first()

        dashboardPage.assertCourseNotShown(nonFavorite)

        dashboardPage.editFavorites()
        editFavoritesPage.assertCourseDisplayed(nonFavorite)
        editFavoritesPage.assertCourseNotFavorited(nonFavorite)
        editFavoritesPage.toggleCourse(nonFavorite)
        editFavoritesPage.assertCourseFavorited(nonFavorite)

        Espresso.pressBack()

        dashboardPage.assertDisplaysCourse(nonFavorite)
    }

    @Test
    @TestMetaData(Priority.P1, FeatureCategory.DASHBOARD, TestCategory.INTERACTION)
    fun testDashboardCourses_removeFavorite() {
        // Un-starring should remove course from favorite list

        val data = getToDashboard(courseCount = 2, favoriteCourseCount = 2)
        val favorite = data.courses.values.filter {x -> x.isFavorite}.first()

        dashboardPage.assertDisplaysCourse(favorite)

        dashboardPage.editFavorites()
        editFavoritesPage.assertCourseDisplayed(favorite)
        editFavoritesPage.assertCourseFavorited(favorite)
        editFavoritesPage.toggleCourse(favorite)
        editFavoritesPage.assertCourseNotFavorited(favorite)

        Espresso.pressBack()

        dashboardPage.assertCourseNotShown(favorite)


    }

    @Test
    @TestMetaData(Priority.P1, FeatureCategory.DASHBOARD, TestCategory.INTERACTION)
    fun testDashboardCourses_seeAll() {
        // Clicking "See all" should show all courses

        // Verify that favorite courses are showing
        val data = getToDashboard(courseCount=2, favoriteCourseCount=1)
        val favorites = data.courses.values.filter {x -> x.isFavorite}
        val all = data.courses.values
        dashboardPage.assertDisplaysCourses()
        for(course in favorites) {
            dashboardPage.assertDisplaysCourse(course)
        }

        // Verify that all courses show in "See All" page
        dashboardPage.assertSeeAllDisplayed()
        dashboardPage.clickSeeAll()
        for(course in all) {
            allCoursesPage.assertDisplaysCourse(course)
        }
        allCoursesPage.assertDisplaysAllCourses()
    }

    @Test
    @TestMetaData(Priority.P1, FeatureCategory.DASHBOARD, TestCategory.INTERACTION)
    fun testDashboardAnnouncement_refresh() {
        // Pull to refresh loads new announcements
        val data = getToDashboard(courseCount = 1, favoriteCourseCount = 1) // No announcements initially
        dashboardPage.assertAnnouncementsGone()
        val announcement = data.addAccountNotification()
        dashboardPage.refresh()
        dashboardPage.assertAnnouncementShowing(announcement)
    }

    @Test
    @TestMetaData(Priority.P1, FeatureCategory.DASHBOARD, TestCategory.INTERACTION)
    fun testDashboardAnnouncement_dismiss() {
        // Tapping dismiss should remove the announcement. Refresh should not display it again.
        val data = getToDashboard(courseCount = 1,favoriteCourseCount = 1,announcementCount = 1)
        val announcement = data.accountNotifications.values.first()
        dashboardPage.assertAnnouncementShowing(announcement)
        dashboardPage.dismissAnnouncement()
        dashboardPage.assertAnnouncementsGone()
        dashboardPage.refresh()
        dashboardPage.assertAnnouncementsGone()
    }

    @Test
    @TestMetaData(Priority.P1, FeatureCategory.DASHBOARD, TestCategory.INTERACTION)
    fun testDashboardAnnouncement_view() {
        // Tapping global announcement displays the content
        val data = getToDashboard(courseCount = 1,favoriteCourseCount = 1,announcementCount = 1)
        val announcement = data.accountNotifications.values.first()
        dashboardPage.assertAnnouncementShowing(announcement)
        dashboardPage.tapAnnouncementAndAssertDisplayed(announcement)
    }

    @Test
    @TestMetaData(Priority.P0, FeatureCategory.DASHBOARD, TestCategory.INTERACTION, false, FeatureCategory.COURSE)
    fun testDashboardCourses_tappingCourseCardDisplaysCourseBrowser() {
        // Tapping on a course card opens course browser page
        val data = getToDashboard(courseCount = 1, favoriteCourseCount = 1)
        val course = data.courses.values.first()
        dashboardPage.selectCourse(course)

        courseBrowserPage.assertPageObjects()
        courseBrowserPage.assertTitleCorrect(course)
        var tabs = data.courseTabs[course.id]
        assertNotNull("Expected course tabs to be populated", tabs)
        for(tab in tabs!!) {
            courseBrowserPage.assertTabDisplayed(tab)
        }
    }

    @Test
    @TestMetaData(Priority.P1, FeatureCategory.DASHBOARD, TestCategory.INTERACTION, false, FeatureCategory.COURSE)
    fun testDashboardCourses_gradeIsDisplayedWhenShowGradesIsSelected() {
        // [Student] Grade is displayed when 'Show Grades' (located in navigation drawer) is selected
        getToDashboard(courseCount = 1, favoriteCourseCount = 1)
        dashboardPage.setShowGrades(true)
        dashboardPage.assertShowsGrades()
    }

    @Test
    @TestMetaData(Priority.P1, FeatureCategory.DASHBOARD, TestCategory.INTERACTION, false, FeatureCategory.COURSE)
    fun testDashboardCourses_gradeIsNotDisplayedWhenShowGradesIsDeSelected() {
        // [Student] Grade is NOT displayed when 'Show Grades' (located in navigation drawer) is de-selected
        getToDashboard(courseCount = 1, favoriteCourseCount = 1)
        dashboardPage.setShowGrades(false)
        dashboardPage.assertHidesGrades()
    }

    private fun getToDashboard(
            courseCount: Int = 1,
            pastCourseCount: Int = 0,
            favoriteCourseCount: Int = 0,
            announcementCount: Int = 0): MockCanvas {
        val data = MockCanvas.init(
                studentCount = 1,
                courseCount = courseCount,
                pastCourseCount = pastCourseCount,
                favoriteCourseCount = favoriteCourseCount,
                accountNotificationCount = announcementCount)
        val student = data.students[0]
        val token = data.tokenFor(student)!!
        tokenLogin(data.domain, token, student)
        dashboardPage.waitForRender()
        return data
    }
}
