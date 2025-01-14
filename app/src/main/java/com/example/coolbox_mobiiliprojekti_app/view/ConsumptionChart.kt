package com.example.coolbox_mobiiliprojekti_app.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coolbox_mobiiliprojekti_app.R
import com.example.coolbox_mobiiliprojekti_app.datastore.UserPreferences
import com.example.coolbox_mobiiliprojekti_app.ui.theme.CoolBoxmobiiliprojektiAppTheme
import com.example.coolbox_mobiiliprojekti_app.ui.theme.GraphKwhColor
import com.example.coolbox_mobiiliprojekti_app.ui.theme.GraphKwhColorDark
import com.example.coolbox_mobiiliprojekti_app.ui.theme.GraphTempColor
import com.example.coolbox_mobiiliprojekti_app.ui.theme.GraphTempColorDark
import com.example.coolbox_mobiiliprojekti_app.viewmodel.ConsumptionViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.CartesianChartHost
import com.patrykandpatrick.vico.compose.chart.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.chart.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.chart.layer.rememberLineSpec
import com.patrykandpatrick.vico.compose.chart.layout.fullWidth
import com.patrykandpatrick.vico.compose.chart.rememberCartesianChart
import com.patrykandpatrick.vico.compose.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.component.shape.dashedShape
import com.patrykandpatrick.vico.compose.component.shape.shader.color
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.layout.HorizontalLayout
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders
import com.patrykandpatrick.vico.core.model.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.model.ExtraStore
import com.patrykandpatrick.vico.core.model.columnSeries
import com.patrykandpatrick.vico.core.model.lineSeries


@Composable
fun ConsumptionChart(
    consumptionStatsData: Map<String, Float?>?,
    temperatureStatsData: Map<String, Float?>?,
    isLandscape: Boolean
) {

    // Haetaan viewmodel
    val viewModel: ConsumptionViewModel = viewModel()

    // Haetaan datastoresta darkmode asetus
    val context = LocalContext.current
    val pref = UserPreferences(context)
    val darkTheme = pref.getDarkMode.collectAsState(isSystemInDarkTheme()).value

    // Luodaan modelProducer, joka vastaa chartin datan käsittelystä
    val modelProducer = remember { CartesianChartModelProducer.build() }

    // Avain labelien tallentamiseen extraStoreen
    val labelListKey = remember { ExtraStore.Key<List<String>>() }

    // Määritetään akselin arvojen muotoilu
    val valueFormatterString =
            AxisValueFormatter<AxisPosition.Horizontal.Bottom> { x, chartValues, _ ->
        chartValues.model.extraStore[labelListKey][x.toInt()]
    }

    // Käynnistetään effect, joka reagoi consumptionStatsData:n ja temperatureStatsData:n muutoksiin
    LaunchedEffect(key1 = consumptionStatsData, key2 = temperatureStatsData) {
        viewModel.consumptionStatsData?.let { consumptionStatsData ->
            viewModel.temperatureStatsData?.let { temperatureStatsData ->
                // Yritetään suorittaa transaktio modelProducerilla
                modelProducer.tryRunTransaction {
                    // Haetaan datan avaimet ja arvot listoiksi
                    val dates = consumptionStatsData.keys.toList()
                    val consumptions = consumptionStatsData.values.toList()
                    val temperatures = temperatureStatsData.values.toList()

                    // Muotoillaan päivämäärät päivän nimiksi
                    val datesFormatted = formatToDateToDayOfWeek(dates)

                    // Luodaan sarakkeet kulutusdatalle
                    columnSeries {
                        series(consumptions)
                    }
                    // Luodaan viivat lämpötiladatalle
                    lineSeries {
                        series(temperatures)
                    }
                    // Päivitetään extras lisäämällä päivämäärät
                    updateExtras {
                        it[labelListKey] = datesFormatted
                    }
                }
            }
        }
    }

    // Luodaan teema
    CoolBoxmobiiliprojektiAppTheme {
            // Pinta, joka kattaa koko näytön leveyden
            Surface(
                modifier = Modifier
                    .fillMaxWidth(if (isLandscape) {
                        0.5f
                    } else {
                        1f
                    }),
                color = MaterialTheme.colorScheme.background
            ) {
                // Sarake, joka täyttää koko leveyden
                Column(modifier = Modifier
                    .fillMaxWidth()
                ) {
                    // Kortti, joka toimii paneelina
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentSize(Alignment.Center),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {

                        // Teksti paneelin keskelle
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 20.dp),
                            fontSize = 20.sp,
                            text = stringResource(R.string.con_graph_title),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                            // CartesianChartHost, joka sisältää chartin
                            CartesianChartHost(
                                chart = rememberCartesianChart(
                                    rememberColumnCartesianLayer(
                                        columns = listOf(
                                            rememberLineComponent(
                                                color = if (darkTheme) GraphKwhColorDark else GraphKwhColor,
                                                thickness = 8.dp, // Adjust as needed
                                            )
                                        ),
                                    ),
                                    rememberLineCartesianLayer(
                                        lines = listOf(
                                            rememberLineSpec(
                                                shader = DynamicShaders.color(
                                                    if (darkTheme) GraphTempColorDark else GraphTempColor
                                                )
                                            )
                                        ),
                                    ),
                                    startAxis = rememberStartAxis(
                                        label = rememberAxisLabelComponent(
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        ),
                                        axis = rememberLineComponent(
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        ),
                                        guideline = rememberLineComponent(
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            shape = remember {
                                                Shapes.dashedShape(
                                                    shape = Shapes.rectShape,
                                                    dashLength = 3.dp,
                                                    gapLength = 3.dp,
                                                )
                                            },
                                        )
                                    ),
                                    bottomAxis = rememberBottomAxis(
                                        label = rememberAxisLabelComponent(
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        ),
                                        axis = rememberLineComponent(
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        ),
                                        valueFormatter = valueFormatterString,
                                        itemPlacer = remember {
                                            AxisItemPlacer.Horizontal.default(
                                                spacing = 1,
                                                addExtremeLabelPadding = true
                                            )
                                        },
                                    ),
                                ),
                                marker = rememberMarker(),
                                modelProducer = modelProducer,
                                horizontalLayout = HorizontalLayout.fullWidth(),
                            )
                        }
                    } // Paneeli loppuu

                } // Sarake loppuu
            }
        }

