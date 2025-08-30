package com.a1.sitesync.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a1.sitesync.data.database.model.Survey
import com.a1.sitesync.data.database.model.SurveyWithPhotos
import com.a1.sitesync.data.repository.SiteSyncRepository
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SiteSyncViewModel(
    private val repository: SiteSyncRepository
) : ViewModel() {
    // Expose all surveys
    val surveys: StateFlow<List<SurveyWithPhotos>> = repository.getAllSurveys()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Get a single survey
    fun getSurvey(id: String) = repository.getSurveyById(id)

    // Create a new survey
    fun createSurvey(
        surveyorId: String,
        clientName: String,
        siteAddress: String?,
        gateType: String,
        dimensions: com.a1.sitesync.data.database.model.Dimensions,
        provisions: com.a1.sitesync.data.database.model.Provisions,
        openingDirection: String?,
        recommendedGate: String?,
        photoPaths: List<String>
    ) = viewModelScope.launch {
        repository.createNewSurvey(
            surveyorId,
            clientName,
            siteAddress,
            gateType,
            dimensions,
            provisions,
            openingDirection,
            recommendedGate,
            photoPaths
        )
    }

    // Update an existing survey
    fun updateSurvey(survey: Survey) = viewModelScope.launch {
        repository.updateSurvey(survey)
    }

    // Delete a survey
    fun deleteSurvey(survey: Survey) = viewModelScope.launch {
        repository.deleteSurvey(survey)
    }

    // Trigger a full sync
    fun syncData() = viewModelScope.launch {
        repository.performDataSync()
    }

    // PDF report file path state
    private val _reportFilePath = MutableStateFlow<String?>(null)
    val reportFilePath: StateFlow<String?> = _reportFilePath

    /**
     * Generates a PDF report with photos for the given survey and updates reportFilePath.
     */
    fun generateReport(context: Context, surveyId: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val surveyWithPhotos = repository.getSurveyById(surveyId).first()
            val doc = PDDocument()
            surveyWithPhotos.photos.forEach { photo ->
                val imgFile = File(photo.localFilePath)
                if (imgFile.exists()) {
                    val pdImage = PDImageXObject.createFromFileByContent(imgFile, doc)
                    val page = PDPage(PDRectangle.LETTER)
                    doc.addPage(page)
                    val content = PDPageContentStream(doc, page)
                    // scale image to fit
                    val scale = 0.5f
                    val imgWidth = pdImage.width * scale
                    val imgHeight = pdImage.height * scale
                    content.drawImage(pdImage, 20f, page.mediaBox.height - imgHeight - 20f, imgWidth, imgHeight)
                    content.close()
                }
            }
            val reportFile = File(context.filesDir, "${surveyId}_report.pdf")
            doc.save(reportFile)
            doc.close()
            _reportFilePath.value = reportFile.absolutePath
        } catch (e: Exception) {
            // handle error
            e.printStackTrace()
        }
    }

    // Expose Photo addition
    fun addPhotoToSurvey(surveyId: String, localFilePath: String) = viewModelScope.launch {
        repository.addPhotoToSurvey(surveyId, localFilePath)
    }
}
