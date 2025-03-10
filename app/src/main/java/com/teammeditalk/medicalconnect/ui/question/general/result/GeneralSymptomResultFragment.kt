package com.teammeditalk.medicalconnect.ui.question.inner.result

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.teammeditalk.medicalconnect.R
import com.teammeditalk.medicalconnect.base.BaseFragment
import com.teammeditalk.medicalconnect.databinding.FragmentGeneralSymptomResultBinding
import com.teammeditalk.medicalconnect.ui.question.QuestionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class GeneralSymptomResultFragment :
    BaseFragment<FragmentGeneralSymptomResultBinding>(
        FragmentGeneralSymptomResultBinding::inflate,
    ) {
    private val viewModel: QuestionViewModel by activityViewModels()

    private fun showSymptomResult() {
        lifecycleScope.launch {
            viewModel.additionalInput.collectLatest {
                val options =
                    TranslatorOptions
                        .Builder()
                        .setSourceLanguage(TranslateLanguage.KOREAN)
                        .setTargetLanguage(TranslateLanguage.ENGLISH)
                        .build()
                val koreanEnglishTranslator = Translation.getClient(options)

                koreanEnglishTranslator
                    .translate(it)
                    .addOnSuccessListener {
                        binding.layoutAdditionalInput.tvInput.text = it
                    }.addOnFailureListener { exception ->
                        exception.printStackTrace()
                        Timber.d("Failed to translate :${exception.message}")
                    }
            }
        }

        lifecycleScope.launch {
            viewModel.selectedRegion.collectLatest {
                binding.layoutCurrentSymptom.tvSymptomRegion.text = it
            }
        }

        lifecycleScope.launch {
            viewModel.selectedSymptom.collectLatest {
                binding.layoutCurrentSymptom.tvSymptomTitle.text = it.first
                binding.layoutCurrentSymptom.tvSymptomContent.text = it.second
            }
        }
        lifecycleScope.launch {
            viewModel.selectedDate.collectLatest {
                binding.layoutCurrentSymptom.tvSymptomStartDate.text = it
            }
        }

        lifecycleScope.launch {
            viewModel.selectedDegree.collectLatest {
                binding.layoutCurrentSymptom.tvSymptomDegree.text = it.toString()
            }
        }

        lifecycleScope.launch {
            viewModel.selectedType.collectLatest {
                binding.layoutCurrentSymptom.tvSymptomType.text = it.toString()
            }
        }

        lifecycleScope.launch {
            viewModel.selectedWorseList.collectLatest {
                binding.layoutCurrentSymptom.tvSymptomWorseList.text = it.toString()
            }
        }
        lifecycleScope.launch {
            viewModel.selectedOtherList.collectLatest {
                binding.layoutCurrentSymptom.tvOtherSymptom.text = it.toString()
            }
        }

        lifecycleScope.launch {
            viewModel.userHealthInfo.collectLatest {
                with(binding) {
                    layoutUserHealthInfo.tvDrug.text = it.drugList.toString()
                    layoutUserHealthInfo.tvDisease.text = it.diseaseList.toString()
                    layoutUserHealthInfo.tvFamilyDisease.text = it.familyDiseaseList.toString()
                    layoutUserHealthInfo.tvAllergy.text = it.allergyList.toString()
                }
            }
        }
    }

    override fun onBindLayout() {
        super.onBindLayout()
        showSymptomResult()
        binding.btnBack.setOnClickListener { findNavController().navigate(R.id.generalAdditionalInputFragment) }
        binding.btnClose.setOnClickListener { findNavController().navigate(R.id.homeFragment3) }
        binding.hospitalType.btnGoToMap.setOnClickListener {
            findNavController().navigate(R.id.action_generalSymptomResultFragment_to_mapFragment3)
        }

        viewModel.saveGeneralResponse()
    }
}
