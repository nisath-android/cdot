package org.naminfo.activities.main.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.naminfo.R

class HelpFragment : Fragment() {

    private val faqList = listOf(
        "How do I open the app?" to "After installation, find the mobion_fs application in your mobile app drawer and open it.",
        "How do I log in?" to "On the login screen, enter your username and password. Tap the Login button to access the app.",
        "How do I access contacts?" to "On the main screen, tap the Contacts button located below. Tap the Telephone button to display your PBX contacts.",
        "How do I make a call?" to "Use the dial pad in the app to enter the user's number or any PBX number you want to call.",
        "How do I make an audio conference call?" to "Enter 5500 on the dial pad to initiate an audio conference call.",
        "How do I make a video conference call?" to "Enter 3500 on the dial pad to initiate a video conference call."
    )

    private val expandedStates = BooleanArray(faqList.size) { false }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_help, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val faqListView = view.findViewById<ListView>(R.id.faqListView)
        val backButton = view.findViewById<ImageView>(R.id.back_help)

        backButton.setOnClickListener {
            val navController = findNavController()
            navController.popBackStack()
        }
        faqListView.adapter = object : BaseAdapter() {
            override fun getCount(): Int = faqList.size

            override fun getItem(position: Int): Pair<String, String> = faqList[position]

            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val itemView = convertView
                    ?: LayoutInflater.from(requireContext()).inflate(
                        R.layout.faq_item,
                        parent,
                        false
                    )

                val questionText = itemView.findViewById<TextView>(R.id.questionText)
                val answerText = itemView.findViewById<TextView>(R.id.answerText)
                val toggleButton = itemView.findViewById<Button>(R.id.toggleButton)

                val (question, answer) = getItem(position)

                questionText.text = question
                answerText.text = answer

                toggleButton.text = if (expandedStates[position]) "-" else "+"
                answerText.visibility = if (expandedStates[position]) View.VISIBLE else View.GONE

                toggleButton.setOnClickListener {
                    expandedStates[position] = !expandedStates[position]
                    notifyDataSetChanged()
                }

                return itemView
            }
        }
    }
}
