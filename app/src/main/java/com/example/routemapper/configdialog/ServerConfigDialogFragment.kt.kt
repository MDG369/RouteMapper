import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import android.view.View
import android.widget.EditText
import com.example.routemapper.R

class ServerConfigDialogFragment : DialogFragment() {

    interface ServerConfigListener {
        fun onServerConfigInput(ip: String = "192.168.1.49", port: String = "8080")
    }

    private var listener: ServerConfigListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? ServerConfigListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_input, null)
        val editTextIP = dialogView.findViewById<EditText>(R.id.editTextIP)
        val editTextPort = dialogView.findViewById<EditText>(R.id.editTextPort)

        val sharedPreferences = requireActivity().getSharedPreferences("ServerConfigPrefs", Context.MODE_PRIVATE)
        val savedIp = sharedPreferences.getString("server_ip", "192.168.1.49")
        val savedPort = sharedPreferences.getString("server_port", "8080")

        // Set saved values in EditText fields
        editTextIP.setText(savedIp)
        editTextPort.setText(savedPort)

        builder.setView(dialogView)
            .setTitle("Server Configuration")
            .setPositiveButton("OK") { dialog, id ->
                val ip = editTextIP?.text.toString()
                val port = editTextPort?.text.toString()

                // Save the input to SharedPreferences
                with(sharedPreferences.edit()) {
                    putString("server_ip", ip)
                    putString("server_port", port)
                    apply()
                }

                listener?.onServerConfigInput(ip, port)
            }
            .setNegativeButton("Cancel") { dialog, id ->
                dialog.dismiss()
            }

        return builder.create()
    }
}
