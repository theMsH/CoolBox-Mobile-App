package com.example.coolbox_mobiiliprojekti_app.view

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coolbox_mobiiliprojekti_app.R
import com.example.coolbox_mobiiliprojekti_app.ui.theme.BottomAppBarColor
import com.example.coolbox_mobiiliprojekti_app.ui.theme.TopAppBarColor
import com.example.coolbox_mobiiliprojekti_app.viewmodel.ConsumptionViewModel
import kotlinx.coroutines.Job
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumptionScreen(
    onMenuClick: () -> Job,
    goBack: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation

    // Determine if the current orientation is landscape
    val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE

    val viewModel: ConsumptionViewModel = viewModel()
    // Alusta nykyinen aikaväli tilamuuttuja
    var currentTimeInterval by remember { mutableStateOf(TimeInterval.DAYS) }

    // Määritä nykyisen viikon ensimmäinen päivä
    var currentWeekStartDate by remember { mutableStateOf(LocalDate.now().startOfWeek()) }
    var currentWeekEndDate = currentWeekStartDate.plusDays(6)

    // Haetaan localeForMonthAndDay-muuttujaan arvo käyttöjärjestelmän kielen
    // mukaan. Nimetään käyttöliittymässä näkyvä kuukausi ja päivä
    // localeForMonthAndDay-muuttujaan tallennetulla kielellä.
    val systemLocale = Locale.getDefault()
    var localeForMonthAndDay = Locale("us", "US")
    var monthName = currentWeekStartDate.month.getDisplayName(TextStyle.FULL, localeForMonthAndDay).capitalize()
    var dayName = currentWeekStartDate.dayOfWeek.getDisplayName(TextStyle.FULL, localeForMonthAndDay).capitalize()

    if (systemLocale.language == "fi") {
        localeForMonthAndDay = Locale("fi", "FI")
        // Koska kuukausien ja päivien nimet ovat suomenkielisessä
        // käännöksessä partitiivimuodossa, pudotetaan kaksi viimeisintä
        // kirjainta pois.
        monthName = currentWeekStartDate.month.getDisplayName(TextStyle.FULL, localeForMonthAndDay).dropLast(2)
        dayName = currentWeekStartDate.dayOfWeek.getDisplayName(TextStyle.FULL, localeForMonthAndDay).dropLast(2)
    }

    // Päivitä kulutustilastot ja lämpötilatilastot haettaessa dataa
    LaunchedEffect(key1 = currentWeekStartDate, key2 = Unit) {
        // Päivitä datan haku sen mukaan, mikä aikaväli on valittu
        when (currentTimeInterval) {
            TimeInterval.HOURS -> {
                viewModel.consumptionFetchData(TimeInterval.HOURS, currentWeekStartDate)
            }

            TimeInterval.DAYS -> {
                viewModel.consumptionFetchData(TimeInterval.DAYS, currentWeekStartDate)
            }

            TimeInterval.WEEKS -> {
                viewModel.consumptionFetchData(TimeInterval.WEEKS, currentWeekStartDate)
            }

            TimeInterval.MONTHS -> {
                viewModel.consumptionFetchData(TimeInterval.MONTHS, currentWeekStartDate)
            }

            TimeInterval.MAIN -> {
                viewModel.consumptionFetchData(TimeInterval.MAIN, currentWeekStartDate)
            }
        }
    }

    // Määritä näytön sisältö
    Scaffold(
        topBar = {
            // Yläpalkki
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TopAppBarColor
                ),
                // Navigointinappi (takaisin)
                navigationIcon = {
                    IconButton(onClick = { goBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                // Otsikko
                title = { Text(text = stringResource(R.string.consumption_title)) },
                // Toiminnot
                actions = {
                    IconButton(onClick = { onMenuClick() }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Menu"
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Alapalkki
            BottomAppBar(
                containerColor = BottomAppBarColor
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Kuukausi-nappi
                    Button(
                        enabled = currentTimeInterval != TimeInterval.MONTHS,
                        onClick = {
                            // Lisää logiikka kuukausidataan siirtymiseen
                            val currentMonthStartDate = LocalDate.now().withDayOfMonth(1)
                            viewModel.consumptionFetchData(
                                TimeInterval.MONTHS,
                                currentMonthStartDate
                            )
                            currentTimeInterval = TimeInterval.MONTHS
                        }
                    ) {
                        Text(text = stringResource(R.string.months_text))
                    }
                    // Viikko-nappi
                    Button(
                        enabled = currentTimeInterval != TimeInterval.WEEKS,
                        onClick = {
                            // Hae viikkodata
                            val currentWeekNumber = LocalDate.now()
                                .get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
                            val currentYear = LocalDate.now().year

                            val firstDayOfWeek = LocalDate.ofYearDay(currentYear, 1)
                                .with(
                                    WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear(),
                                    currentWeekNumber.toLong()
                                )
                                .with(DayOfWeek.MONDAY)

                            currentWeekStartDate = firstDayOfWeek
                            currentWeekEndDate = firstDayOfWeek.plusDays(6)

                            viewModel.consumptionFetchData(TimeInterval.WEEKS, currentWeekStartDate)

                            currentTimeInterval = TimeInterval.WEEKS

                        }
                    ) {
                        Text(text = stringResource(R.string.weeks_text))
                    }

                    // Päivä-nappi
                    Button(
                        enabled = currentTimeInterval != TimeInterval.DAYS,
                        onClick = {
                            // Hae päivädata
                            currentWeekStartDate = LocalDate.now().startOfWeek()
                            viewModel.consumptionFetchData(TimeInterval.DAYS, currentWeekStartDate)
                            currentTimeInterval = TimeInterval.DAYS
                        }
                    ) {
                        Text(text = stringResource(R.string.days_text))
                    }

                    // Tunti-nappi
                    Button(
                        enabled = currentTimeInterval != TimeInterval.HOURS,
                        onClick = {
                            // Hae tuntidata
                            currentWeekStartDate = LocalDate.now()
                            viewModel.consumptionFetchData(TimeInterval.HOURS, currentWeekStartDate)
                            currentTimeInterval = TimeInterval.HOURS
                        }
                    ) {
                        Text(text = stringResource(R.string.hours_text))
                    }
                }
            }
        }
    ) { innerPaddings ->
        // Näytön sisältö
        Box(
            modifier = Modifier
                .padding(innerPaddings)
                .fillMaxSize()
        ) {
            if (isLandscape) {
                when {
                    // Latauspalkki
                    viewModel.consumptionChartState.value.loading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )

                    // Näytä sisältö
                    else -> Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                        ) {
                            // Piirrä kaavio
                            ConsumptionChart(
                                viewModel.consumptionStatsData,
                                viewModel.temperatureStatsData,
                                isLandscape = isLandscape
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Nuoli vasemmalle
                                IconButton(
                                    onClick = {
                                        when (currentTimeInterval) {
                                            TimeInterval.DAYS -> {
                                                // Siirry edellisen viikon alkuun
                                                currentWeekStartDate =
                                                    currentWeekStartDate.minusWeeks(1)
                                                        .startOfWeek()

                                            }

                                            TimeInterval.HOURS -> {
                                                // Siirry edellisen päivän alkuun
                                                currentWeekStartDate =
                                                    currentWeekStartDate.minusDays(1)

                                            }

                                            TimeInterval.WEEKS -> {
                                                // Siirry edellisen kuukauden alkuun
                                                currentWeekStartDate =
                                                    currentWeekStartDate.minusMonths(1)
                                                        .startOfWeek()

                                            }

                                            TimeInterval.MONTHS -> {
                                                // Siirry edellisen vuoden alkuun
                                                currentWeekStartDate =
                                                    currentWeekStartDate.minusYears(1)
                                            }

                                            TimeInterval.MAIN -> TODO()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null
                                    )
                                }

                                // Näytä päivämäärä
                                Text(
                                    text = when (currentTimeInterval) {
                                        TimeInterval.DAYS -> {
                                            "${currentWeekStartDate.dayOfMonth}/${currentWeekStartDate.monthValue} " +
                                                    "- ${currentWeekEndDate.dayOfMonth}/${currentWeekEndDate.monthValue}"
                                        }

                                        TimeInterval.HOURS -> {
                                            dayName + " (${currentWeekStartDate.dayOfMonth}/${currentWeekStartDate.monthValue})"
                                        }

                                        TimeInterval.WEEKS -> {
                                            "$monthName ${currentWeekStartDate.year}"
                                        }

                                        TimeInterval.MONTHS -> {
                                            "${currentWeekStartDate.year}"
                                        }

                                        TimeInterval.MAIN -> TODO()
                                    },
                                    fontSize = 30.sp
                                )

                                // Nuoli oikealle
                                IconButton(
                                    onClick = {
                                        // Siirry seuraavan aikavälin alkuun
                                        when (currentTimeInterval) {
                                            TimeInterval.DAYS -> {
                                                currentWeekStartDate =
                                                    currentWeekStartDate.plusWeeks(1)
                                                        .startOfWeek()
                                            }

                                            TimeInterval.HOURS -> {
                                                currentWeekStartDate =
                                                    currentWeekStartDate.plusDays(1)
                                            }

                                            TimeInterval.WEEKS -> {
                                                currentWeekStartDate =
                                                    currentWeekStartDate.plusMonths(1)
                                                        .startOfWeek()
                                            }

                                            TimeInterval.MONTHS -> {
                                                currentWeekStartDate =
                                                    currentWeekStartDate.plusYears(1)
                                            }

                                            TimeInterval.MAIN -> TODO()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Näytä yhteenveto
                            Text(
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .align(Alignment.CenterHorizontally),
                                text = stringResource(R.string.total_con_text) + ":  ${
                                    String.format(
                                        Locale.US,
                                        "%.2f",
                                        viewModel.consumptionStatsData?.values?.sum() ?: 0f
                                    )
                                } kwh",
                                fontSize = 20.sp
                            )

                            // Näytä keskiarvo
                            Text(
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .align(Alignment.CenterHorizontally),
                                text = stringResource(R.string.avg_con_text) + ":  ${
                                    String.format(
                                        Locale.US,
                                        "%.2f",
                                        viewModel.consumptionStatsData?.values?.average() ?: 0f
                                    )
                                } kwh",
                                fontSize = 20.sp
                            )

                            // Näytä keskimääräinen lämpötila
                            Text(
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .align(Alignment.CenterHorizontally),
                                text = stringResource(R.string.avg_temp_text) + ":  ${
                                    String.format(
                                        Locale.US,
                                        "%.1f",
                                        viewModel.temperatureStatsData?.values?.average() ?: 0f
                                    )
                                } °C",
                                textAlign = TextAlign.Center,
                                fontSize = 20.sp, // Aseta tekstin koko 16 sp (scaled pixels)
                            )
                        }
                    }
                }
            } else {
                when {
                    // Latauspalkki
                    viewModel.consumptionChartState.value.loading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )

                    // Näytä sisältö
                    else -> Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // Piirrä kaavio
                        ConsumptionChart(
                            viewModel.consumptionStatsData,
                            viewModel.temperatureStatsData,
                            isLandscape = isLandscape
                        )
                        Spacer(Modifier.height(30.dp))

                        // Piirrä nuolinapit ja niiden välissä oleva aikaväli
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Nuoli vasemmalle
                            IconButton(
                                onClick = {
                                    when (currentTimeInterval) {
                                        TimeInterval.DAYS -> {
                                            // Siirry edellisen viikon alkuun
                                            currentWeekStartDate =
                                                currentWeekStartDate.minusWeeks(1).startOfWeek()

                                        }

                                        TimeInterval.HOURS -> {
                                            // Siirry edellisen päivän alkuun
                                            currentWeekStartDate = currentWeekStartDate.minusDays(1)

                                        }

                                        TimeInterval.WEEKS -> {
                                            // Siirry edellisen kuukauden alkuun
                                            currentWeekStartDate =
                                                currentWeekStartDate.minusMonths(1).startOfWeek()

                                        }

                                        TimeInterval.MONTHS -> {
                                            // Siirry edellisen vuoden alkuun
                                            currentWeekStartDate =
                                                currentWeekStartDate.minusYears(1)
                                        }

                                        TimeInterval.MAIN -> TODO()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null
                                )
                            }
                            Spacer(modifier = Modifier.weight(0.5f))

                            // Näytä päivämäärä
                            Text(
                                text = when (currentTimeInterval) {
                                    TimeInterval.DAYS -> {
                                        "${currentWeekStartDate.dayOfMonth}/${currentWeekStartDate.monthValue} " +
                                                "- ${currentWeekEndDate.dayOfMonth}/${currentWeekEndDate.monthValue}"
                                    }

                                    TimeInterval.HOURS -> {
                                        dayName + " (${currentWeekStartDate.dayOfMonth}/${currentWeekStartDate.monthValue})"
                                    }

                                    TimeInterval.WEEKS -> {
                                        "$monthName ${currentWeekStartDate.year}"
                                    }

                                    TimeInterval.MONTHS -> {
                                        "${currentWeekStartDate.year}"
                                    }

                                    TimeInterval.MAIN -> TODO()
                                },
                                fontSize = 30.sp
                            )
                            Spacer(modifier = Modifier.weight(0.5f))

                            // Nuoli oikealle
                            IconButton(
                                onClick = {
                                    // Siirry seuraavan aikavälin alkuun
                                    when (currentTimeInterval) {
                                        TimeInterval.DAYS -> {
                                            currentWeekStartDate =
                                                currentWeekStartDate.plusWeeks(1).startOfWeek()
                                        }

                                        TimeInterval.HOURS -> {
                                            currentWeekStartDate = currentWeekStartDate.plusDays(1)
                                        }

                                        TimeInterval.WEEKS -> {
                                            currentWeekStartDate =
                                                currentWeekStartDate.plusMonths(1).startOfWeek()
                                        }

                                        TimeInterval.MONTHS -> {
                                            currentWeekStartDate = currentWeekStartDate.plusYears(1)
                                        }

                                        TimeInterval.MAIN -> TODO()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 60.dp), // Add horizontal padding to the entire column
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                // Display total consumption text
                                Text(
                                    modifier = Modifier
                                        .padding(vertical = 16.dp)
                                        .weight(1f), // Adjust weight to reduce stretching
                                    text = stringResource(R.string.total_con_text) + ":",
                                    fontSize = 30.sp
                                )

                                // Display total consumption data
                                Text(
                                    modifier = Modifier
                                        .padding(vertical = 16.dp)
                                        .weight(1f), // Adjust weight to keep data closer to the text
                                    text = "${String.format(
                                            Locale.US,
                                            "%.2f",
                                            viewModel.consumptionStatsData?.values?.sum() ?: 0f
                                        )
                                    } kwh",
                                    fontSize = 30.sp,
                                    textAlign = TextAlign.End // Ensure data text aligns to the end
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth()) {
                                // Display average consumption text
                                Text(
                                    modifier = Modifier
                                        .padding(vertical = 16.dp)
                                        .weight(1f), // Consistent weight for alignment
                                    text = stringResource(R.string.avg_con_text) + ":",
                                    fontSize = 30.sp
                                )

                                // Display average consumption data
                                Text(
                                    modifier = Modifier
                                        .padding(vertical = 16.dp)
                                        .weight(1f), // Consistent weight for alignment
                                    text = "${String.format(
                                            Locale.US,
                                            "%.2f",
                                            viewModel.consumptionStatsData?.values?.average() ?: 0f
                                        )
                                    } kwh",
                                    fontSize = 30.sp,
                                    textAlign = TextAlign.End
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth()) {
                                // Display average temperature text
                                Text(
                                    modifier = Modifier
                                        .padding(vertical = 16.dp)
                                        .weight(1f), // Consistent weight for alignment
                                    text = stringResource(R.string.avg_temp_text) + ":",
                                    fontSize = 30.sp,
                                    lineHeight = 40.sp
                                )

                                // Display average temperature data
                                Text(
                                    modifier = Modifier
                                        .padding(vertical = 16.dp)
                                        .weight(1f), // Consistent weight for alignment
                                    text = "${String.format(
                                            Locale.US,
                                            "%.1f",
                                            viewModel.temperatureStatsData?.values?.average() ?: 0f
                                        )
                                    } °C",
                                    fontSize = 30.sp,
                                    lineHeight = 40.sp,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}









