import com.digitalpersona.onetouch.DPFPDataPurpose;
import com.digitalpersona.onetouch.DPFPFeatureSet;
import com.digitalpersona.onetouch.DPFPFingerIndex;
import com.digitalpersona.onetouch.DPFPGlobal;
import com.digitalpersona.onetouch.DPFPSample;
import com.digitalpersona.onetouch.DPFPTemplate;
import com.digitalpersona.onetouch.capture.DPFPCapture;
import com.digitalpersona.onetouch.capture.DPFPCapturePriority;
import com.digitalpersona.onetouch.capture.event.DPFPDataEvent;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusEvent;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusListener;
import com.digitalpersona.onetouch.processing.DPFPEnrollment;
import com.digitalpersona.onetouch.processing.DPFPFeatureExtraction;
import com.digitalpersona.onetouch.processing.DPFPImageQualityException;
import com.digitalpersona.onetouch.readers.DPFPReadersCollection;
import java.awt.event.ActionEvent;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;

/**
 *
 * 
 * @author uncle-samba
 */
@SuppressWarnings("serial")
public class FrameDis extends javax.swing.JFrame {

	DPFPTemplate template;
	DPFPFingerIndex fingerIndex;
	String report = "";

	/**
	 * @param args
	 *            the command line arguments
	 */

	public static void main(String[] args) {
		// Set the Nimbus look and feel /
		//
		// If Nimbus (introduced in Java SE 6) is not available, stay with the default
		// look and feel.

		// For details see
		// http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException ex) {
			java.util.logging.Logger.getLogger(FrameDis.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(FrameDis.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(FrameDis.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(FrameDis.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}
		//
		/* Create and display the form */
		java.awt.EventQueue.invokeLater(() -> {
			FrameDis f = new FrameDis();
			// byte[] b = template1.serialize();
			f.setVisible(true);
		});
	}

	public DPFPTemplate getTemplate(String reader, int nFinger) {
		StringBuilder sb = new StringBuilder();
		DPFPTemplate temp = null;
		DPFPFeatureExtraction extraction;
		DPFPEnrollment enrollment;
		DPFPFingerIndex finger;
		try {
			finger = DPFPFingerIndex.LEFT_THUMB;
			extraction = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
			enrollment = DPFPGlobal.getEnrollmentFactory().createEnrollment();

			while (enrollment.getFeaturesNeeded() > 0) {
				DPFPSample sample = getSample(reader, String.format("Scan your %s finger (%d remaining)\n",
						"left pinky", enrollment.getFeaturesNeeded()));
				if (sample == null)
					continue;
				DPFPFeatureSet featureSet;
				try {
					featureSet = extraction.createFeatureSet(sample, DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);
					enrollment.addFeatures(featureSet);
				} catch (DPFPImageQualityException e) {
					setText("Featureset Error [getTemplate]:", String.format("Bad image quality: \"%s\". Try again. \n",
							e.getCaptureFeedback().toString()));
				}
			}
			temp = enrollment.getTemplate();
			setText("message [getTemplate]", "Finger enrolled!");
		} catch (UnsatisfiedLinkError err) {
			setText("UnsatisfiedLinkError [getTemplate]:", err.getLocalizedMessage());
		} catch (InterruptedException ex) {
			setText("InterruptedException [getTemplate]:", ex.getLocalizedMessage());
			Logger.getLogger(FrameDis.class.getName()).log(Level.SEVERE, null, ex);
		}
		return temp;
	}

	public DPFPSample getSample(String reader, String prompt) throws InterruptedException {
		final LinkedBlockingQueue samples = new LinkedBlockingQueue<>();
		DPFPCapture capture = DPFPGlobal.getCaptureFactory().createCapture();
		capture.setReaderSerialNumber(reader);
		capture.setPriority(DPFPCapturePriority.CAPTURE_PRIORITY_LOW);
		capture.addDataListener((DPFPDataEvent dpfpde) -> {
			if (dpfpde != null && dpfpde.getSample() != null) {
				try {
					samples.put(dpfpde.getSample());
				} catch (Exception e) {
				}
			}
		});

		capture.addReaderStatusListener(new DPFPReaderStatusListener() {
			int lastStatus = DPFPReaderStatusEvent.READER_CONNECTED;

			@Override
			public void readerConnected(DPFPReaderStatusEvent dpfprs) {
				if (lastStatus != dpfprs.getReaderStatus())
					setText("Reader Status", "Reader Connected!");
				lastStatus = dpfprs.getReaderStatus();
			}

			@Override
			public void readerDisconnected(DPFPReaderStatusEvent dpfprs) {
				if (lastStatus != dpfprs.getReaderStatus())
					setText("Reader Status", "Reader Disconnected");
				lastStatus = dpfprs.getReaderStatus();
			}
		});

		try {
			capture.startCapture();
			return (DPFPSample) samples.take();
		} catch (Exception e) {
			setText("Error Capture Starting!:", e.getLocalizedMessage() + "\n");
			throw e;
		} finally {
			setText("Capture Stopping:", prompt);
			capture.stopCapture();
		}
	}

	public void listReaders() {
		DPFPReadersCollection readers;
		try {
			readers = DPFPGlobal.getReadersFactory().getReaders();
			if (readers == null || readers.isEmpty()) {
				setText("Error", "No readers Found!");
				return;
			}
			StringBuilder sb = new StringBuilder();
			readers.stream().forEach((d) -> {
				sb.append("reader: ").append(d.getSerialNumber());
			});
			setText("readers", sb.toString());
		} catch (Exception exception) {
			setText("Error", exception.getLocalizedMessage());
		}
	}

	/**
	 * 
	 * Creates new form FrameDis
	 */
	public FrameDis() {
		initComponents();
	}

	/**
	 * 
	 * This method is called from within the constructor to initialize the form.
	 * 
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * 
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
	//
	private void initComponents() {

		jButton1 = new javax.swing.JButton();
		jLabel1 = new javax.swing.JLabel();

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

		jButton1.setText("Click Me To Start Capture ");
		jButton1.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton1ActionPerformed(evt);
			}
		});

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(layout.createSequentialGroup().addComponent(jButton1).addGap(0, 346, Short.MAX_VALUE))
						.addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
								layout.createSequentialGroup().addContainerGap()
										.addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addContainerGap()));
		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addComponent(jButton1)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 466, Short.MAX_VALUE)
						.addContainerGap()));

		jLabel1.setText("<body style='width:200px;'" + jLabel1.getText() + "");

		pack();
	}//

	private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
		setText("Sample", "App Started!\n");
		listReaders();
		DPFPTemplate template1 = getTemplate(null, 1);
		setText("Success", "Finger Print gotten Finally!!");
	}

	public void setText(String title, String msg) {
		String s = new StringBuilder().append(" ").append(title).append("\n").append(" ").append(msg).append(" ")
				.append("\n===================================================\n").toString();
		report += s.toString();
		jLabel1.setText("<body style='width:200px;'" + report + "");
		this.repaint();
	}

	public JButton getBtn() {
		return this.jButton1;
	}

	// Variables declaration - do not modify
	private javax.swing.JButton jButton1;
	private javax.swing.JLabel jLabel1;
	// End of variables declaration
}