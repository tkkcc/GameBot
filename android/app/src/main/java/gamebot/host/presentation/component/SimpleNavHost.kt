package gamebot.host.presentation.component

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import kotlin.reflect.KClass
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SimpleNavHost(
    navController: NavHostController, startDestination: String, builder: NavGraphBuilder.() -> Unit
) {
    // right animation speed for fadeIn and fadeOut
    // TODO: cursor/keyboard/dropdown disappearing lagging
    //

    val offsetDivider = 8
    val pushDurationMillis = 250
    val popDurationMillis = 250

    NavHost(modifier=Modifier.fillMaxSize(),
        navController = navController, startDestination = startDestination, enterTransition = {
            EnterTransition.None

            fadeIn(
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
//            animationSpec = tween(pushDurationMillis)
            ) +
                    slideIntoContainer(
//                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
//                    animationSpec = tween(pushDurationMillis),
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        initialOffset = { it / offsetDivider })
//            expandHorizontally()
//            fadeIn() + expandIn ()
//            slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Down)
//            scaleInToFitContainer()

//            fadeIn()
        }, exitTransition = {
//        ExitTransition.Hold
//        ExitTransition.Hold
//        shrinkVertically()
//        fadeOut()


            fadeOut(
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
//            animationSpec = tween(pushDurationMillis)
            ) +
                    slideOutOfContainer(
//            animationSpec = tween(pushDurationMillis),
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        targetOffset = { it / offsetDivider })

//            shrinkHorizontally()

//            scaleOutToFitContainer()
//            fadeOut()+ shrinkOut()
        }, popEnterTransition = {
            EnterTransition.None

            fadeIn(
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
//            animationSpec = tween(popDurationMillis)

            ) +
                    slideIntoContainer(
//            animationSpec = tween(popDurationMillis),
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        initialOffset = { it / offsetDivider })


        }, popExitTransition = {
            ExitTransition.None
            fadeOut(
//            animationSpec = tween(popDurationMillis)
                spring(stiffness = Spring.StiffnessMedium)

            ) +
                    slideOutOfContainer(
//            animationSpec = tween(popDurationMillis),
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        targetOffset = { it / offsetDivider })
        }, builder = builder
    )

}
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SimpleNavHost(
    navController: NavHostController, startDestination: Any, builder: NavGraphBuilder.() -> Unit
) {
    // right animation speed for fadeIn and fadeOut
    // TODO: cursor/keyboard/dropdown disappearing lagging
    //

    val offsetDivider = 8
    val pushDurationMillis = 250
    val popDurationMillis = 250

    NavHost(modifier=Modifier.fillMaxSize(),
        navController = navController, startDestination = startDestination, enterTransition = {
        EnterTransition.None

        fadeIn(
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
//            animationSpec = tween(pushDurationMillis)
        ) +
                slideIntoContainer(
//                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
//                    animationSpec = tween(pushDurationMillis),
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    initialOffset = { it / offsetDivider })
//            expandHorizontally()
//            fadeIn() + expandIn ()
//            slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Down)
//            scaleInToFitContainer()

//            fadeIn()
    }, exitTransition = {
//        ExitTransition.Hold
//        ExitTransition.Hold
//        shrinkVertically()
//        fadeOut()


        fadeOut(
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
//            animationSpec = tween(pushDurationMillis)
        ) +
                slideOutOfContainer(
//            animationSpec = tween(pushDurationMillis),
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    targetOffset = { it / offsetDivider })

//            shrinkHorizontally()

//            scaleOutToFitContainer()
//            fadeOut()+ shrinkOut()
    }, popEnterTransition = {
        EnterTransition.None

        fadeIn(
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
//            animationSpec = tween(popDurationMillis)

        ) +
                slideIntoContainer(
//            animationSpec = tween(popDurationMillis),
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    initialOffset = { it / offsetDivider })


    }, popExitTransition = {
        ExitTransition.None
        fadeOut(
//            animationSpec = tween(popDurationMillis)
            spring(stiffness = Spring.StiffnessMedium)

        ) +
                slideOutOfContainer(
//            animationSpec = tween(popDurationMillis),
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    targetOffset = { it / offsetDivider })
    }, builder = builder
    )
}