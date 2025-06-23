import {
  onDocumentCreated,
  onDocumentUpdated,
} from "firebase-functions/v2/firestore";
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { setGlobalOptions } from "firebase-functions/v2";

initializeApp();
setGlobalOptions({ region: "southamerica-east1" });

export const onarticleapproved = onDocumentUpdated(
  "articles/{articleId}",
  async (event) => {
    const beforeData = event.data?.before.data();
    const afterData = event.data?.after.data();

    if (
      !beforeData ||
      !afterData ||
      beforeData.approved === afterData.approved
    ) {
      console.log("Nenhuma mudança relevante no status de aprovação.");
      return null;
    }

    if (beforeData.approved === false && afterData.approved === true) {
      const authorId = afterData.authorId;
      if (!authorId) {
        console.log(`Artigo ${event.params.articleId} não tem authorId.`);
        return null;
      }

      const userDoc = await getFirestore()
        .collection("users")
        .doc(authorId)
        .get();
      const tokens = userDoc.data()?.fcmTokens;

      if (tokens && tokens.length > 0) {
        console.log(`Enviando notificação de aprovação para ${authorId}`);
        const payload = {
          notification: {
            title: "Seu artigo foi aprovado! ✅",
            body: `Parabéns! Seu artigo "${afterData.title}" foi publicado.`,
          },
        };
        const message = {
          notification: payload.notification,
          tokens: tokens,
        };
        return getMessaging().sendEachForMulticast(message);
      } else {
        console.log(`Autor ${authorId} não possui tokens para notificar.`);
        return null;
      }
    }
    return null;
  }
);

export const onnewreview = onDocumentCreated(
  "articles/{articleId}/reviews/{reviewId}",
  async (event) => {
    const review = event.data?.data();
    if (!review) {
      console.log("Dados da nova avaliação não encontrados.");
      return null;
    }

    const articleId = event.params.articleId;

    const articleDoc = await getFirestore()
      .collection("articles")
      .doc(articleId)
      .get();
    const article = articleDoc.data();

    if (!article || !article.authorId || article.authorId === review.userId) {
      console.log(
        "Autor não encontrado ou é o mesmo da avaliação. Sem notificação."
      );
      return null;
    }

    const userDoc = await getFirestore()
      .collection("users")
      .doc(article.authorId)
      .get();
    const tokens = userDoc.data()?.fcmTokens;

    if (tokens && tokens.length > 0) {
      console.log(
        `Enviando notificação de nova avaliação para ${article.authorId}`
      );
      const payload = {
        notification: {
          title: "Nova avaliação no seu artigo! ⭐",
          body: `${review.userName} avaliou seu artigo "${article.title}".`,
        },
      };
      const message = {
        notification: payload.notification,
        tokens: tokens,
      };
      return getMessaging().sendEachForMulticast(message);
    } else {
      console.log(
        `Autor ${article.authorId} não possui tokens para notificar.`
      );
      return null;
    }
  }
);

export const onnewreport = onDocumentCreated(
  "reports/{reportId}",
  async (event) => {
    const report = event.data?.data();
    if (!report) {
      console.log("Dados da denúncia não encontrados.");
      return null;
    }

    const adminUsersSnapshot = await getFirestore()
      .collection("users")
      .where("isAdmin", "==", true)
      .get();

    if (adminUsersSnapshot.empty) {
      console.log("Nenhum administrador encontrado para notificar.");
      return null;
    }

    const adminTokens = adminUsersSnapshot.docs.flatMap(
      (doc) => doc.data().fcmTokens || []
    );

    if (adminTokens.length > 0) {
      console.log(
        `Enviando notificação de denúncia para ${adminTokens.length} tokens de admin.`
      );
      const payload = {
        notification: {
          title: "Nova Denúncia Recebida ⚠️",
          body: `Um(a) ${report.contentType} foi denunciado(a). Motivo: "${report.reason}"`,
        },
      };
      const message = {
        notification: payload.notification,
        tokens: adminTokens,
      };
      return getMessaging().sendEachForMulticast(message);
    }
    return null;
  }
);

export const onuserbanned = onDocumentUpdated(
  "users/{userId}",
  async (event) => {
    const beforeData = event.data?.before.data();
    const afterData = event.data?.after.data();

    if (!beforeData || !afterData) {
      return null;
    }

    if (beforeData.isBanned === false && afterData.isBanned === true) {
      const tokens = afterData.fcmTokens;
      if (tokens && tokens.length > 0) {
        console.log(
          `Enviando notificação de banimento para ${event.params.userId}`
        );
        const payload = {
          notification: {
            title: "Aviso de Conta",
            body: "Sua conta no HiveDocs foi banida por um administrador devido a violações dos termos de uso.",
          },
        };
        const message = {
          notification: payload.notification,
          tokens: tokens,
        };
        return getMessaging().sendEachForMulticast(message);
      }
    }
    return null;
  }
);

export const onnewreply = onDocumentCreated(
  "articles/{articleId}/reviews/{reviewId}/replies/{replyId}",
  async (event) => {
    const reply = event.data?.data();
    if (!reply) {
      return null;
    }

    const { articleId, reviewId } = event.params;

    const reviewDoc = await getFirestore()
      .collection("articles")
      .doc(articleId)
      .collection("reviews")
      .doc(reviewId)
      .get();
    const review = reviewDoc.data();

    if (!review || !review.userId || review.userId === reply.userId) {
      return null;
    }

    const userToNotifyDoc = await getFirestore()
      .collection("users")
      .doc(review.userId)
      .get();
    const tokens = userToNotifyDoc.data()?.fcmTokens;

    if (tokens && tokens.length > 0) {
      console.log(`Enviando notificação de resposta para ${review.userId}`);
      const articleTitle =
        (
          await getFirestore().collection("articles").doc(articleId).get()
        ).data()?.title || "um artigo";
      const payload = {
        notification: {
          title: "Você recebeu uma nova resposta! 💬",
          body: `${reply.userName} respondeu ao seu comentário no artigo "${articleTitle}".`,
        },
      };
      const message = {
        notification: payload.notification,
        tokens: tokens,
      };
      return getMessaging().sendEachForMulticast(message);
    }
    return null;
  }
);
