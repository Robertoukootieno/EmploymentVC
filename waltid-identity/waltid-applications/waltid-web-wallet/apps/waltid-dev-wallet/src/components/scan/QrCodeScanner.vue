<script setup>
import QrScanner from "qr-scanner";
import CenterMain from "@waltid-web-wallet/components/CenterMain.vue";
import LoadingIndicator from "@waltid-web-wallet/components/loading/LoadingIndicator.vue";
import {
  ArrowPathRoundedSquareIcon,
  ChevronUpDownIcon,
  LightBulbIcon,
  VideoCameraIcon,
  VideoCameraSlashIcon,
  XMarkIcon,
} from "@heroicons/vue/24/outline";
import WaltButton from "@waltid-web-wallet/components/buttons/WaltButton.vue";
import { isSiopRequest } from "@waltid-web-wallet/composables/siop-requests.ts"

const emit = defineEmits(["request"]);

const isLoading = ref(true);
const videoStarted = ref(false);
const scanned = ref(false);
const error = ref({});
const noError = ref(true);
const scannerVideo = ref(null);
let qrScanner = null;

function throwError(newError) {
  isLoading.value = false;
  error.value = newError;
  noError.value = false;
  console.error(error.value.title);
  console.error(error.value.message);
}

async function startVideo(tries = 0) {
  if (tries >= 1) {
    console.error("QR Scanner: Giving up after too many tries.");
    throwError({
      title: "Could not start camera",
      message:
        "Could not initialize your camera. Please make sure you have accepted the camera permission in your browser.",
    });
    return;
  }

  if (await QrScanner.hasCamera()) {
    console.log("Starting video");
    console.log("Camera list", await QrScanner.listCameras());
    try {
      console.log("Creating QR scanner...");
      qrScanner = new QrScanner(
        scannerVideo.value,
        (result) => {
          scanned.value = true;
          console.log(result);
          const scannedText = result.data;

          if (isSiopRequest(scannedText)) {
            qrScanner.stop();
            emit("request", scannedText);
          } else {
            console.log("Invalid QR");
          }
        },
        {
          highlightScanRegion: true,
          highlightCodeOutline: true,
          returnDetailedScanResult: true,
        },
      );
      console.log("Starting QR scanner...");
      isLoading.value = false;
      await qrScanner.start();
      videoStarted.value = true;
      console.log("Started QR scanner!");
    } catch (exception) {
      console.error("QR Error:", exception);
      console.log("Restarting...");
      window.setTimeout(async () => {
        await startVideo(++tries);
      }, 500);
    }
  } else {
    throwError({
      title: "No camera",
      message: "You do not have any camera available.",
    });
  }
}

function logErrors(promise) {
  promise.catch(console.error);
}

function paintOutline(detectedCodes, ctx) {
  for (const detectedCode of detectedCodes) {
    const [firstPoint, ...otherPoints] = detectedCode.cornerPoints;

    ctx.strokeStyle = "red";
    ctx.beginPath();
    ctx.moveTo(firstPoint.x, firstPoint.y);
    for (const { x, y } of otherPoints) {
      ctx.lineTo(x, y);
    }
    ctx.lineTo(firstPoint.x, firstPoint.y);
    ctx.closePath();
    ctx.stroke();
  }
}

if (process.client) {
  onMounted(async () => {
    await startVideo();
  });
}

// Async usage examples
console.log(await QrScanner.listCameras(true));
qrScanner?.hasFlash();
qrScanner?.isFlashOn();
qrScanner?.turnFlashOn();
qrScanner?.turnFlashOff();
qrScanner?.toggleFlash();
</script>
